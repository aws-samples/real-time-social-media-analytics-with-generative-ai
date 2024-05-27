import streamlit as st
from streamlit_cognito_auth import CognitoAuthenticator
import requests
import base64
import json
import argparse
import os

# Create page
st.set_page_config(
    page_title="Bedrock ChatBot",
    page_icon=":robot_face:",
)

# Parse arguments
parser = argparse.ArgumentParser()
parser.add_argument("--pool_id", type=str, help="Cognito User Pool ID")
parser.add_argument("--app_client_id", type=str, help='Cognito User Pool App Client ID')
parser.add_argument("--app_client_secret", type=str, help='Cognito User Pool App Client Secret')
parser.add_argument("--bedrock_api", type=str, help="API GW pointing to the Lambda function that invokes Bedrock's api")
parser.add_argument("--flink_rules_api", type=str, help='API GW which writes to Kinesis data stream to modify rules')
parser.add_argument("--kinesis_social_api", type=str, help='API GW which writes to Kinesis data stream to the my social kinesis media stream')
args = parser.parse_args()
pool_id = args.pool_id
app_client_id = args.app_client_id
app_client_secret = args.app_client_secret
bedrock_api = args.bedrock_api
flink_rules_api = args.flink_rules_api
kinesis_social_api = args.kinesis_social_api

try:
    args = parser.parse_args()
except SystemExit as e:
    # This exception will be raised if --help or invalid command line arguments
    # are used. Currently streamlit prevents the program from exiting normally
    # so we have to do a hard exit.
    os._exit(e.code)

if 'kinesis_social_api' not in st.session_state:
    st.session_state['kinesis_social_api'] = kinesis_social_api
if 'pool_id' not in st.session_state:
    st.session_state['pool_id'] = pool_id
if 'app_client_id' not in st.session_state:
    st.session_state['app_client_id'] = app_client_id
if 'app_client_secret' not in st.session_state:
    st.session_state['app_client_secret'] = app_client_secret

authenticator = CognitoAuthenticator(
    pool_id=pool_id,
    app_client_id=app_client_id,
    app_client_secret=app_client_secret,
    use_cookies=False
)

is_logged_in = authenticator.login()
if not is_logged_in:
    st.stop()
st.session_state['logged_in'] = True
st.session_state['authenticator'] = authenticator

# get access token from cognito
credentials = authenticator.get_credentials()
id_token = credentials.id_token

if 'id_token' not in st.session_state:
    st.session_state['id_token'] = id_token

def logout():
    authenticator.logout()

def send_message(rule, message, api):
    stream_name = "rules-stream"
    headers = {"Authorization": f"Bearer {id_token}"}
    body = {"rule": rule,
            "key":message,
            "id":"1"}

    payload = {
        "StreamName": stream_name,
        "Data": base64.b64encode(json.dumps(body).encode()).decode(),
        "PartitionKey": str(1)  # Use random number as partition key
    }

    response = requests.post(api, json=payload,headers=headers)


# Add title
st.markdown("<h2 style='text-align: center;'>💬 Uncover insights in Twitter with Amazon Bedrock and Amazon Managed Service for Apache Flink</h2>", unsafe_allow_html=True)
st.markdown("""
To query and get insights on your simulated tweets:

1. In the Streamlit application sidebar, click on the Bedrock Chatbot application and ensure the "Use Twitter Index" option is disabled. This will allow the application to query and display data from the index containing your simulated tweets.
2. Type your query in the message box below.

**Note:** If you want to analyze real Twitter data instead, simply enable the "Use Twitter Index" option in the sidebar in the Bedrock Chatbot app. This will switch the application to query and display data from the index containing real tweets ingested from the Twitter API.""")
# OpenSearch index selection in sidebar. App rerun on each change
use_twitter_index = st.sidebar.checkbox("Use Twitter Index", key="twitter-index")

# Create form to batch options and prevent app rerun on each change
with st.sidebar.form(key ='Options', border=False):
    # Add sidebar title
    title = st.title("Options")
    # Add options to the sidecar
    twitter_api_key = st.text_input("Enter Twitter Bearer Token:", '')
    query_input = st.text_input("Enter Query Term:", '')
    timer_input = st.text_input("Enter Timer:", '')
    submit = st.form_submit_button("Submit")

with st.sidebar:
    st.text(f"Welcome!")
    st.button("Logout", "logout_btn", on_click=logout)

# Submit available options using send_message function
if submit:
    if query_input:
        send_message("query", query_input, flink_rules_api)
    if timer_input:
        send_message("timer", timer_input, flink_rules_api)
    if twitter_api_key:
        send_message("apiKey", twitter_api_key, flink_rules_api)

    # if any of the options are available print Options submitted! else print no options added
    if query_input and timer_input and twitter_api_key:
        st.sidebar.success("Options submitted!")
    else:
        st.sidebar.warning("Missing options!")


# Initialize chat
with st.chat_message("assistant"):
    st.write("How can I help you?")

# Initialize chat history
if "messages" not in st.session_state:
    st.session_state.messages = []

# Display chat messages from history on app rerun
for message in st.session_state.messages:
    with st.chat_message(message["role"]):
        st.markdown(message["content"])

# React to user input
if prompt := st.chat_input("Ask me anything related to tweets"):

    # Display user message in chat message container
    st.chat_message("user").markdown(prompt)
    # Add user message to chat history
    st.session_state.messages.append({"role": "user", "content": prompt})
    # Set the index flag based on the toggle button state
    index_flag = "twitter-index" if use_twitter_index else "custom-index"
    headers = {"Authorization": f"Bearer {id_token}"}

    api_response = requests.post(bedrock_api, json={"message": prompt, "index": index_flag}, headers=headers)

    try:
        # Try to parse API response as JSON
        api_response_content = api_response.json().get("response", "No response")
    except ValueError:
        # If parsing as JSON fails, use the raw content
        api_response_content = api_response.text

    response = f"{api_response_content}"

    # Display assistant response in chat message container
    with st.chat_message("assistant"):
        st.markdown(response)
    # Add assistant response to chat history
    st.session_state.messages.append({"role": "assistant", "content": response})
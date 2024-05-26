import streamlit as st
from streamlit_cognito_auth import CognitoAuthenticator
import requests
import json
import base64
from datetime import datetime
import random
import argparse
import os

st.set_page_config(
    page_title="My Social Media",
    page_icon="📈"
)

# Initialize chat history
if "logged_in" not in st.session_state:
    st.session_state['logged_in'] = False

# Check if user is logged in
if not st.session_state['logged_in']:
    st.info('Please login in the Bedrock Chatbot page to continue')
    st.stop()

# Initialize chat history
if "kinesis_messages" not in st.session_state:
    st.session_state.kinesis_messages = []
if "pool_id" not in st.session_state:
    st.session_state['pool_id'] = ''
if 'app_client_id' not in st.session_state:
    st.session_state['app_client_id'] = ''
if 'app_client_secret' not in st.session_state:
    st.session_state['app_client_secret'] = ''

pool_id = st.session_state['pool_id']
app_client_id = st.session_state['app_client_id']
app_client_secret = st.session_state['app_client_secret']

if 'authenticator' not in st.session_state:
    st.session_state['authenticator'] = []

authenticator = st.session_state['authenticator']

if 'id_token' not in st.session_state:
    st.session_state['id_token'] = ''

id_token = st.session_state['id_token']

def logout():
    st.session_state['logged_in'] = False
    authenticator.logout()

# Function to send message to the API
def send_message(message, api):
    stream_name = "rag-stream"
    headers = {"Authorization": f"Bearer {id_token}"}
    timestamp = datetime.utcnow().strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"
    data = {"text": message, "created_at": timestamp}
    payload = {
        "StreamName": stream_name,
        "Data": base64.b64encode(json.dumps(data).encode()).decode(),
        "PartitionKey": str(random.randint(1, 1000))  # Use random number as partition key
    }

    try:
        response = requests.post(api, json=payload,headers=headers)
        if response.status_code == 200:
            st.success("Message sent successfully!")
        else:
            st.error(f"Failed to send message. Error code: {response.status_code}")
    except Exception as e:
        st.error(f"An error occurred: {e}")


st.markdown("# My Social Media")
st.markdown(""" ### Send Simulated Tweets with My Social Media App

With the My Social Media App, you can compose and send simulated tweets, just like you would on a real social media platform. These simulated tweets will be processed and stored in a separate index within our data pipeline, allowing you to analyze and query them instead of real Twitter data.

To send a simulated tweet:

1. Type your message in the below text box.
2. Send message to submit your simulated tweet.

Your simulated tweet will be ingested into our data pipeline and stored in a dedicated index within the Amazon OpenSearch Service cluster.""")

# Display chat messages from history on app rerun
for message in st.session_state.kinesis_messages:
    with st.chat_message(message["role"]):
        st.markdown(message["content"])

with st.sidebar:
    st.text(f"Welcome!")
    st.button("Logout", "logout_btn", on_click=logout)

# React to user input
if prompt := st.chat_input("Compose a new message"):

    # Display user message in chat message container
    st.chat_message("user").markdown(prompt)
    # Add user message to chat history
    st.session_state.kinesis_messages.append({"role": "user", "content": prompt})
    send_message(prompt, st.session_state['kinesis_social_api'])

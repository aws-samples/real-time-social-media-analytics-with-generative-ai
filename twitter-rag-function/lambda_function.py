import json
import boto3
from langchain.prompts import PromptTemplate
from langchain.chains import RetrievalQA
from langchain_community.embeddings.bedrock import BedrockEmbeddings
from langchain_community.vectorstores import OpenSearchVectorSearch
from langchain_community.chat_models import BedrockChat
from opensearchpy import RequestsHttpConnection
import os

region = os.environ['region']
bedrock_runtime = boto3.client("bedrock-runtime", region)

def lambda_handler(event, context):

    body = json.loads(event["body"])
    message = body.get("message", "")
    print(f"Query: {message}")

    embeddings = BedrockEmbeddings(model_id="amazon.titan-embed-text-v1", client=bedrock_runtime)

    index = body.get("index", "")

    if index != "custom-index":
        aos_index = os.environ['aosIndex']
        text_field = 'tweet'
    else:
        aos_index = os.environ['aosCustomIndex']
        text_field = 'text'

    os_client = OpenSearchVectorSearch(
        index_name=aos_index,
        embedding_function=embeddings,
        http_auth=(os.environ['aosUser'], os.environ['aosPassword']),
        opensearch_url=os.environ['aosDomain'],
        timeout=300,
        use_ssl=True,
        verify_certs=True,
        connection_class=RequestsHttpConnection,
    )

    model_kwargs={"temperature": 0, "max_tokens": 4096}

    llm = BedrockChat(
        model_id="anthropic.claude-3-haiku-20240307-v1:0",
        client=bedrock_runtime,
        model_kwargs=model_kwargs
    )

    template = """As a helpful agent that is an expert analysing tweets, please answer the question using only the provided tweets from the context in <context></context> tags.
                  If you don't see valuable information on the tweets provided in the context in <context></context> tags, say you don't have enough tweets related to the question.
                  Cite the relevant context you used to build your answer. Print in a bullet point list the top most influential tweets from the context at the end of the response.
                  Ignore negative tweets in your analysis.
    
    Find below some examples:
    <example1>
    question: 
    What are the main challenges or concerns mentioned in tweets about using Bedrock as a generative AI service on AWS, and how can they be addressed?
    
    answer:
    Based on the tweets provided in the context, the main challenges or concerns mentioned about using Bedrock as a generative AI service on AWS are:

    1. Cost and pricing concerns: Some tweets mention concerns about the cost of using large models on Bedrock and optimizing costs (#awswishlist, partyrock).

    2. Confusing strategy and unclear roadmap: One tweet mentions that Bedrock strategy was confusing and the roadmap was unclear, which led them to use other services instead (2/2).

    3. Privacy and security concerns: Some tweets discuss the need to address privacy and security when using generative AI applications (#GenAI).

    4. Lack of customization and flexibility: One tweet mentions the need for customization and flexibility to experiment with different model types and sizes (#AWS).
    
    To address these concerns:

    1. Leverage AWS's credibility and responsibility for privacy and security to alleviate those concerns (#GenAI).
    
    2. Optimize costs by using cost tracking and multi-tenant architectures to monitor usage and costs (#SaaS, Build an internal SaaS service).
    
    3. Communicate the roadmap and strategy more clearly to address confusion.
    
    4. Provide more customization options and flexibility to experiment with different models.

    Top tweets from context:

    [1] ...
    
    [2] ...
    
    [3] ...
    
    [4] ...
    </example1>
    
    <example2>
    question:
    What are the initial impressions and sentiments surrounding Bedrock as a new generative AI service for AWS, based on Twitter conversations?

    answer:
    Based on the tweets provided in the context, the initial impressions and sentiments surrounding Bedrock as a new generative AI service for AWS seem to be generally positive. Some of the key points include:

    1. Bedrock is seen as providing customers with more choice and flexibility in the types of models they can access for generative AI needs through AWS. Adding models from various partners like Anthropic, AI21, Meta, Cohere, and Stability AI expands the options available.
    
    2. The addition of specific models like those from OpenAI, such as GPT-3, as well as models from other companies like Mistral AI, are highlighted as expanding capabilities for tasks like text summarization, question answering, and code completion.
    
    3. Customers and users express excitement about being able to access these powerful foundation models through Bedrock's APIs and functionality for building and scaling generative AI applications privately and securely on AWS.
    
    4. Several tweets showcase examples of how specific companies are using Bedrock to automate tasks, enhance customer experiences, and streamline operations. This provides evidence that Bedrock enables valuable and practical use cases.
    
    5. The modernization of the Bedrock console interface is noted positively as an improvement.
    
    6. Events and workshops focused on learning and experimenting with Bedrock receive positive feedback, indicating interest in exploring the service.
    
    In general, Bedrock seems to be fulfilling its goal of providing an easy way for customers to leverage generative AI models at scale through AWS, which is welcomed by the Twitter discussions.
    
    The top tweets from the context that helped build this response include:
    
    [1] ...
    
    [2] ...
    
    [3] ...
    
    [4] ...
    
    [5] ...
    </example2>
    
    Human: 
    
    question: {question}
    
    <context>
    {context}
    </context>
    
    Assistant:"""

    prompt = PromptTemplate(input_variables=["context","question"], template=template)

    chain = RetrievalQA.from_chain_type(
        llm=llm,
        verbose=True,
        chain_type="stuff",
        retriever=os_client.as_retriever(
            search_type="similarity",
            search_kwargs={
                "k": 200,
                "space_type": "l2",
                "vector_field": "embeddings",
                "text_field": text_field
            }
        ),
        chain_type_kwargs = {"prompt": prompt}
    )

    answer = chain.invoke({"query": message})
    print(f"Answer: " + answer["result"])

    return {
        'statusCode': 200,
        'body': answer["result"]
    }

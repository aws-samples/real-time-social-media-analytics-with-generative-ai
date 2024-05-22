import os
import json
import urllib3
from base64 import b64encode

def lambda_handler(event, context):

    request_type = event['RequestType']
    match request_type:
        case 'Update':
            print('Update of stack')
        case 'Delete':
            print('Delete Function')
        case 'Create':
        # Retrieve environment variables
            host = os.environ['aosDomain']
            username = os.environ['aosUser']
            password = os.environ['aosPassword']

            # Define the index names and URLs
            index_names = ['twitter-custom-rag-index', 'twitter-rag-index']
            index_urls = [f"{host}/{index_name}" for index_name in index_names]

            # Prepare HTTP headers
            headers = {
                'Content-Type': 'application/json',
                'Authorization': 'Basic ' + b64encode(f"{username}:{password}".encode()).decode('utf-8')
            }

            # Create an HTTP connection pool manager
            http = urllib3.PoolManager()

            # Define the index bodies
            index_bodies = {
                'twitter-custom-rag-index': {
                    "mappings": {
                        "properties": {
                            "embeddings": {
                                "type": "knn_vector",
                                "dimension": 1536,
                                "method": {
                                    "name": "hnsw",
                                    "space_type": "l2",
                                    "engine": "nmslib",
                                    "parameters": {
                                        "ef_construction": 128,
                                        "m": 24
                                    }
                                }
                            },
                            "@timestamp": {
                                "type": "date"
                            },
                            "text": {
                                "type": "text"
                            }
                        }
                    },
                    "settings": {
                        "index": {
                            "knn": True,
                            "number_of_shards": 5,
                            "number_of_replicas": 1
                        }
                    }
                },
                'twitter-rag-index': {
                    "aliases": {},
                    "mappings": {
                        "properties": {
                            "@timestamp": {
                                "type": "date"
                            },
                            "embeddings": {
                                "type": "knn_vector",
                                "dimension": 1536,
                                "method": {
                                    "engine": "nmslib",
                                    "space_type": "l2",
                                    "name": "hnsw",
                                    "parameters": {
                                        "ef_construction": 128,
                                        "m": 24
                                    }
                                }
                            },
                            "impression_count": {
                                "type": "integer"
                            },
                            "likes": {
                                "type": "integer"
                            },
                            "retweet_count": {
                                "type": "integer"
                            },
                            "tweet": {
                                "type": "text"
                            }
                        }
                    },
                    "settings": {
                        "index": {
                            "knn": True,
                            "number_of_shards": 5,
                            "number_of_replicas": 1
                        }
                    }
                }
            }

            results = []

            for index_name, index_url in zip(index_names, index_urls):
                # Check if the index exists
                head_response = http.request(
                    'HEAD',
                    index_url,
                    headers=headers
                )

                if head_response.status == 200:
                    results.append(f"Index {index_name} already exists.")
                elif head_response.status == 404:
                    # If index does not exist, create it
                    create_response = http.request(
                        'PUT',
                        index_url,
                        body=json.dumps(index_bodies[index_name]),
                        headers=headers
                    )

                    if create_response.status == 200:
                        results.append(f"Index {index_name} created successfully!")
                    else:
                        results.append(f"Failed to create index {index_name}: {create_response.data.decode('utf-8')}")
                else:
                    results.append(f"Unexpected error checking index {index_name}: {head_response.data.decode('utf-8')}")

            return {
                'statusCode': 200,
                'body': json.dumps(results)
            }
        case _:
            print('Unexpected Request')
    return
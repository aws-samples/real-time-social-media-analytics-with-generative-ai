"""**Vector store** stores embedded data and performs vector search.

One of the most common ways to store and search over unstructured data is to
embed it and store the resulting embedding vectors, and then query the store
and retrieve the data that are 'most similar' to the embedded query.

**Class hierarchy:**

.. code-block::

    VectorStore --> <name>  # Examples: Annoy, FAISS, Milvus

    BaseRetriever --> VectorStoreRetriever --> <name>Retriever  # Example: VespaRetriever

**Main helpers:**

.. code-block::

    Embeddings, Document
"""  # noqa: E501

from typing import Any


def _import_alibaba_cloud_open_search() -> Any:
    from langchain.vectorstores.alibabacloud_opensearch import AlibabaCloudOpenSearch

    return AlibabaCloudOpenSearch


def _import_alibaba_cloud_open_search_settings() -> Any:
    from langchain.vectorstores.alibabacloud_opensearch import (
        AlibabaCloudOpenSearchSettings,
    )

    return AlibabaCloudOpenSearchSettings


def _import_azure_cosmos_db() -> Any:
    from langchain.vectorstores.azure_cosmos_db import AzureCosmosDBVectorSearch

    return AzureCosmosDBVectorSearch


def _import_elastic_knn_search() -> Any:
    from langchain.vectorstores.elastic_vector_search import ElasticKnnSearch

    return ElasticKnnSearch


def _import_elastic_vector_search() -> Any:
    from langchain.vectorstores.elastic_vector_search import ElasticVectorSearch

    return ElasticVectorSearch


def _import_analyticdb() -> Any:
    from langchain.vectorstores.analyticdb import AnalyticDB

    return AnalyticDB


def _import_annoy() -> Any:
    from langchain.vectorstores.annoy import Annoy

    return Annoy


def _import_atlas() -> Any:
    from langchain.vectorstores.atlas import AtlasDB

    return AtlasDB


def _import_awadb() -> Any:
    from langchain.vectorstores.awadb import AwaDB

    return AwaDB


def _import_azuresearch() -> Any:
    from langchain.vectorstores.azuresearch import AzureSearch

    return AzureSearch


def _import_bageldb() -> Any:
    from langchain.vectorstores.bageldb import Bagel

    return Bagel


def _import_baiducloud_vector_search() -> Any:
    from langchain.vectorstores.baiducloud_vector_search import BESVectorStore

    return BESVectorStore


def _import_cassandra() -> Any:
    from langchain.vectorstores.cassandra import Cassandra

    return Cassandra


def _import_chroma() -> Any:
    from langchain.vectorstores.chroma import Chroma

    return Chroma


def _import_clarifai() -> Any:
    from langchain.vectorstores.clarifai import Clarifai

    return Clarifai


def _import_clickhouse() -> Any:
    from langchain.vectorstores.clickhouse import Clickhouse

    return Clickhouse


def _import_clickhouse_settings() -> Any:
    from langchain.vectorstores.clickhouse import ClickhouseSettings

    return ClickhouseSettings


def _import_dashvector() -> Any:
    from langchain.vectorstores.dashvector import DashVector

    return DashVector


def _import_deeplake() -> Any:
    from langchain.vectorstores.deeplake import DeepLake

    return DeepLake


def _import_dingo() -> Any:
    from langchain.vectorstores.dingo import Dingo

    return Dingo


def _import_docarray_hnsw() -> Any:
    from langchain.vectorstores.docarray import DocArrayHnswSearch

    return DocArrayHnswSearch


def _import_docarray_inmemory() -> Any:
    from langchain.vectorstores.docarray import DocArrayInMemorySearch

    return DocArrayInMemorySearch


def _import_elasticsearch() -> Any:
    from langchain.vectorstores.elasticsearch import ElasticsearchStore

    return ElasticsearchStore


def _import_epsilla() -> Any:
    from langchain.vectorstores.epsilla import Epsilla

    return Epsilla


def _import_faiss() -> Any:
    from langchain.vectorstores.faiss import FAISS

    return FAISS


def _import_hologres() -> Any:
    from langchain.vectorstores.hologres import Hologres

    return Hologres


def _import_lancedb() -> Any:
    from langchain.vectorstores.lancedb import LanceDB

    return LanceDB


def _import_llm_rails() -> Any:
    from langchain.vectorstores.llm_rails import LLMRails

    return LLMRails


def _import_marqo() -> Any:
    from langchain.vectorstores.marqo import Marqo

    return Marqo


def _import_matching_engine() -> Any:
    from langchain.vectorstores.matching_engine import MatchingEngine

    return MatchingEngine


def _import_meilisearch() -> Any:
    from langchain.vectorstores.meilisearch import Meilisearch

    return Meilisearch


def _import_milvus() -> Any:
    from langchain.vectorstores.milvus import Milvus

    return Milvus


def _import_momento_vector_index() -> Any:
    from langchain.vectorstores.momento_vector_index import MomentoVectorIndex

    return MomentoVectorIndex


def _import_mongodb_atlas() -> Any:
    from langchain.vectorstores.mongodb_atlas import MongoDBAtlasVectorSearch

    return MongoDBAtlasVectorSearch


def _import_myscale() -> Any:
    from langchain.vectorstores.myscale import MyScale

    return MyScale


def _import_myscale_settings() -> Any:
    from langchain.vectorstores.myscale import MyScaleSettings

    return MyScaleSettings


def _import_neo4j_vector() -> Any:
    from langchain.vectorstores.neo4j_vector import Neo4jVector

    return Neo4jVector


def _import_opensearch_vector_search() -> Any:
    from langchain.vectorstores.opensearch_vector_search import OpenSearchVectorSearch

    return OpenSearchVectorSearch


def _import_pgembedding() -> Any:
    from langchain.vectorstores.pgembedding import PGEmbedding

    return PGEmbedding


def _import_pgvector() -> Any:
    from langchain.vectorstores.pgvector import PGVector

    return PGVector


def _import_pinecone() -> Any:
    from langchain.vectorstores.pinecone import Pinecone

    return Pinecone


def _import_qdrant() -> Any:
    from langchain.vectorstores.qdrant import Qdrant

    return Qdrant


def _import_redis() -> Any:
    from langchain.vectorstores.redis import Redis

    return Redis


def _import_rocksetdb() -> Any:
    from langchain.vectorstores.rocksetdb import Rockset

    return Rockset


def _import_vespa() -> Any:
    from langchain.vectorstores.vespa import VespaStore

    return VespaStore


def _import_scann() -> Any:
    from langchain.vectorstores.scann import ScaNN

    return ScaNN


def _import_semadb() -> Any:
    from langchain.vectorstores.semadb import SemaDB

    return SemaDB


def _import_singlestoredb() -> Any:
    from langchain.vectorstores.singlestoredb import SingleStoreDB

    return SingleStoreDB


def _import_sklearn() -> Any:
    from langchain.vectorstores.sklearn import SKLearnVectorStore

    return SKLearnVectorStore


def _import_sqlitevss() -> Any:
    from langchain.vectorstores.sqlitevss import SQLiteVSS

    return SQLiteVSS


def _import_starrocks() -> Any:
    from langchain.vectorstores.starrocks import StarRocks

    return StarRocks


def _import_supabase() -> Any:
    from langchain.vectorstores.supabase import SupabaseVectorStore

    return SupabaseVectorStore


def _import_tair() -> Any:
    from langchain.vectorstores.tair import Tair

    return Tair


def _import_tencentvectordb() -> Any:
    from langchain.vectorstores.tencentvectordb import TencentVectorDB

    return TencentVectorDB


def _import_tigris() -> Any:
    from langchain.vectorstores.tigris import Tigris

    return Tigris


def _import_timescalevector() -> Any:
    from langchain.vectorstores.timescalevector import TimescaleVector

    return TimescaleVector


def _import_typesense() -> Any:
    from langchain.vectorstores.typesense import Typesense

    return Typesense


def _import_usearch() -> Any:
    from langchain.vectorstores.usearch import USearch

    return USearch


def _import_vald() -> Any:
    from langchain.vectorstores.vald import Vald

    return Vald


def _import_vearch() -> Any:
    from langchain.vectorstores.vearch import Vearch

    return Vearch


def _import_vectara() -> Any:
    from langchain.vectorstores.vectara import Vectara

    return Vectara


def _import_weaviate() -> Any:
    from langchain.vectorstores.weaviate import Weaviate

    return Weaviate


def _import_zep() -> Any:
    from langchain.vectorstores.zep import ZepVectorStore

    return ZepVectorStore


def _import_zilliz() -> Any:
    from langchain.vectorstores.zilliz import Zilliz

    return Zilliz


def __getattr__(name: str) -> Any:
    if name == "AnalyticDB":
        return _import_analyticdb()
    elif name == "AlibabaCloudOpenSearch":
        return _import_alibaba_cloud_open_search()
    elif name == "AlibabaCloudOpenSearchSettings":
        return _import_alibaba_cloud_open_search_settings()
    elif name == "AzureCosmosDBVectorSearch":
        return _import_azure_cosmos_db()
    elif name == "ElasticKnnSearch":
        return _import_elastic_knn_search()
    elif name == "ElasticVectorSearch":
        return _import_elastic_vector_search()
    elif name == "Annoy":
        return _import_annoy()
    elif name == "AtlasDB":
        return _import_atlas()
    elif name == "AwaDB":
        return _import_awadb()
    elif name == "AzureSearch":
        return _import_azuresearch()
    elif name == "Bagel":
        return _import_bageldb()
    elif name == "BESVectorStore":
        return _import_baiducloud_vector_search()
    elif name == "Cassandra":
        return _import_cassandra()
    elif name == "Chroma":
        return _import_chroma()
    elif name == "Clarifai":
        return _import_clarifai()
    elif name == "ClickhouseSettings":
        return _import_clickhouse_settings()
    elif name == "Clickhouse":
        return _import_clickhouse()
    elif name == "DashVector":
        return _import_dashvector()
    elif name == "DeepLake":
        return _import_deeplake()
    elif name == "Dingo":
        return _import_dingo()
    elif name == "DocArrayInMemorySearch":
        return _import_docarray_inmemory()
    elif name == "DocArrayHnswSearch":
        return _import_docarray_hnsw()
    elif name == "ElasticsearchStore":
        return _import_elasticsearch()
    elif name == "Epsilla":
        return _import_epsilla()
    elif name == "FAISS":
        return _import_faiss()
    elif name == "Hologres":
        return _import_hologres()
    elif name == "LanceDB":
        return _import_lancedb()
    elif name == "LLMRails":
        return _import_llm_rails()
    elif name == "Marqo":
        return _import_marqo()
    elif name == "MatchingEngine":
        return _import_matching_engine()
    elif name == "Meilisearch":
        return _import_meilisearch()
    elif name == "Milvus":
        return _import_milvus()
    elif name == "MomentoVectorIndex":
        return _import_momento_vector_index()
    elif name == "MongoDBAtlasVectorSearch":
        return _import_mongodb_atlas()
    elif name == "MyScaleSettings":
        return _import_myscale_settings()
    elif name == "MyScale":
        return _import_myscale()
    elif name == "Neo4jVector":
        return _import_neo4j_vector()
    elif name == "OpenSearchVectorSearch":
        return _import_opensearch_vector_search()
    elif name == "PGEmbedding":
        return _import_pgembedding()
    elif name == "PGVector":
        return _import_pgvector()
    elif name == "Pinecone":
        return _import_pinecone()
    elif name == "Qdrant":
        return _import_qdrant()
    elif name == "Redis":
        return _import_redis()
    elif name == "Rockset":
        return _import_rocksetdb()
    elif name == "ScaNN":
        return _import_scann()
    elif name == "SemaDB":
        return _import_semadb()
    elif name == "SingleStoreDB":
        return _import_singlestoredb()
    elif name == "SKLearnVectorStore":
        return _import_sklearn()
    elif name == "SQLiteVSS":
        return _import_sqlitevss()
    elif name == "StarRocks":
        return _import_starrocks()
    elif name == "SupabaseVectorStore":
        return _import_supabase()
    elif name == "Tair":
        return _import_tair()
    elif name == "TencentVectorDB":
        return _import_tencentvectordb()
    elif name == "Tigris":
        return _import_tigris()
    elif name == "TimescaleVector":
        return _import_timescalevector()
    elif name == "Typesense":
        return _import_typesense()
    elif name == "USearch":
        return _import_usearch()
    elif name == "Vald":
        return _import_vald()
    elif name == "Vearch":
        return _import_vearch()
    elif name == "Vectara":
        return _import_vectara()
    elif name == "Weaviate":
        return _import_weaviate()
    elif name == "ZepVectorStore":
        return _import_zep()
    elif name == "Zilliz":
        return _import_zilliz()
    elif name == "VespaStore":
        return _import_vespa()
    else:
        raise AttributeError(f"Could not find: {name}")


__all__ = [
    "AlibabaCloudOpenSearch",
    "AlibabaCloudOpenSearchSettings",
    "AnalyticDB",
    "Annoy",
    "AtlasDB",
    "AwaDB",
    "AzureSearch",
    "Bagel",
    "Cassandra",
    "Chroma",
    "Clarifai",
    "Clickhouse",
    "ClickhouseSettings",
    "DashVector",
    "DeepLake",
    "Dingo",
    "DocArrayHnswSearch",
    "DocArrayInMemorySearch",
    "ElasticKnnSearch",
    "ElasticVectorSearch",
    "ElasticsearchStore",
    "Epsilla",
    "FAISS",
    "Hologres",
    "LanceDB",
    "LLMRails",
    "Marqo",
    "MatchingEngine",
    "Meilisearch",
    "Milvus",
    "MomentoVectorIndex",
    "MongoDBAtlasVectorSearch",
    "MyScale",
    "MyScaleSettings",
    "Neo4jVector",
    "OpenSearchVectorSearch",
    "PGEmbedding",
    "PGVector",
    "Pinecone",
    "Qdrant",
    "Redis",
    "Rockset",
    "SKLearnVectorStore",
    "ScaNN",
    "SemaDB",
    "SingleStoreDB",
    "SQLiteVSS",
    "StarRocks",
    "SupabaseVectorStore",
    "Tair",
    "Tigris",
    "TimescaleVector",
    "Typesense",
    "USearch",
    "Vald",
    "Vearch",
    "Vectara",
    "VespaStore",
    "Weaviate",
    "ZepVectorStore",
    "Zilliz",
    "TencentVectorDB",
    "AzureCosmosDBVectorSearch",
]

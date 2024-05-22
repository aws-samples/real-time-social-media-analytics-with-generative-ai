from typing import Any, Dict, List, Optional

from langchain.callbacks.manager import CallbackManagerForRetrieverRun
from langchain.pydantic_v1 import root_validator
from langchain.schema import BaseRetriever, Document
from langchain.utils import get_from_dict_or_env


class YouRetriever(BaseRetriever):
    """`You` retriever that uses You.com's search API.

    To connect to the You.com api requires an API key which
    you can get by emailing api@you.com.
    You can check out our docs at https://documentation.you.com.

    You need to set the environment variable `YDC_API_KEY` for retriever to operate.
    """

    ydc_api_key: str
    k: Optional[int] = None
    endpoint_type: str = "web"

    @root_validator(pre=True)
    def validate_client(
        cls,
        values: Dict[str, Any],
    ) -> Dict[str, Any]:
        values["ydc_api_key"] = get_from_dict_or_env(
            values, "ydc_api_key", "YDC_API_KEY"
        )
        return values

    def _get_relevant_documents(
        self, query: str, *, run_manager: CallbackManagerForRetrieverRun
    ) -> List[Document]:
        import requests

        headers = {"X-API-Key": self.ydc_api_key}
        if self.endpoint_type == "web":
            results = requests.get(
                f"https://api.ydc-index.io/search?query={query}",
                headers=headers,
            ).json()

            docs = []
            for hit in results["hits"]:
                for snippet in hit["snippets"]:
                    docs.append(Document(page_content=snippet))
                    if self.k is not None and len(docs) >= self.k:
                        return docs
            return docs
        elif self.endpoint_type == "snippet":
            results = requests.get(
                f"https://api.ydc-index.io/snippet_search?query={query}",
                headers=headers,
            ).json()
            return [Document(page_content=snippet) for snippet in results]
        else:
            raise RuntimeError(f"Invalid endpoint type provided {self.endpoint_type}")

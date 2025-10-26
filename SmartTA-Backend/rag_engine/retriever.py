import os
from langchain_community.vectorstores import FAISS
from langchain_huggingface import HuggingFaceEmbeddings

DATA_DIR = os.path.join(os.getcwd(), "data")
DB_PATH = os.path.join(DATA_DIR, "faiss_index")

def load_db():
    embeddings = HuggingFaceEmbeddings(model_name="sentence-transformers/all-MiniLM-L6-v2")
    db = FAISS.load_local(DB_PATH, embeddings, allow_dangerous_deserialization=True)
    return db

def retrieve_context(query, top_k=3):
    db = load_db()
    docs = db.similarity_search(query, k=top_k)

    # 返回字典列表，每个包含 source、page、content
    return [
        {
            "source": getattr(d, "metadata", {}).get("source", "unknown"),
            "page": getattr(d, "metadata", {}).get("page", "?"),
            "content": d.page_content
        }
        for d in docs
    ]


if __name__ == "__main__":
    print(retrieve_context("操作系统中的虚拟内存机制"))

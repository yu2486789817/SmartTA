import os
from io import BytesIO
from langchain_community.document_loaders import PyPDFLoader
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_community.vectorstores import FAISS
from langchain_huggingface import HuggingFaceEmbeddings


DATA_DIR = os.path.join(os.getcwd(), "data")
DB_PATH = os.path.join(DATA_DIR, "faiss_index")
os.makedirs(DATA_DIR, exist_ok=True)


def preprocess_pdfs(file_content: BytesIO = None, file_name: str = None, directory: str = None):
    """
    预处理 PDF 文件并增量更新 FAISS 数据库。
    返回结果：{"status": "success" | "error", "message": str, "added_docs": int}
    """

    all_docs = []
    splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)

    try:
        # 单文件模式
        if file_content and file_name:
            temp_path = os.path.join(DATA_DIR, file_name)
            with open(temp_path, "wb") as f:
                f.write(file_content.read())

            loader = PyPDFLoader(temp_path)
            pages = loader.load_and_split(text_splitter=splitter)
            all_docs.extend(pages)
            os.remove(temp_path)

        # 目录模式
        elif directory:
            pdf_files = [f for f in os.listdir(directory) if f.lower().endswith(".pdf")]
            if not pdf_files:
                return {"status": "error", "message": f"No PDF files found in {directory}", "added_docs": 0}

            for pdf in pdf_files:
                try:
                    path = os.path.join(directory, pdf)
                    loader = PyPDFLoader(path)
                    pages = loader.load_and_split(text_splitter=splitter)
                    all_docs.extend(pages)
                except Exception as e:
                    return {"status": "error", "message": f"Failed to process {pdf}: {e}", "added_docs": 0}

        else:
            return {"status": "error", "message": "No valid input provided", "added_docs": 0}

        if not all_docs:
            return {"status": "error", "message": "No text extracted from PDFs", "added_docs": 0}

        # 向量嵌入
        embeddings = HuggingFaceEmbeddings(model_name="sentence-transformers/all-MiniLM-L6-v2")

        # 增量更新或新建
        if os.path.exists(DB_PATH):
            existing_db = FAISS.load_local(DB_PATH, embeddings, allow_dangerous_deserialization=True)
            new_db = FAISS.from_documents(all_docs, embeddings)
            existing_db.merge_from(new_db)
            existing_db.save_local(DB_PATH)
        else:
            db = FAISS.from_documents(all_docs, embeddings)
            os.makedirs(DB_PATH, exist_ok=True)
            db.save_local(DB_PATH)

        return {
            "status": "success",
            "message": f"Database updated successfully. Added {len(all_docs)} new document chunks.",
            "added_docs": len(all_docs)
        }

    except Exception as e:
        return {"status": "error", "message": str(e), "added_docs": 0}

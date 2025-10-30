"""
异常定义模块
统一管理应用异常
"""
from typing import Optional


class SmartTAException(Exception):
    """SmartTA基础异常"""
    
    def __init__(self, message: str, error_code: Optional[str] = None):
        self.message = message
        self.error_code = error_code
        super().__init__(self.message)


class ModelNotFoundError(SmartTAException):
    """模型未找到异常"""
    
    def __init__(self, model_path: str):
        super().__init__(
            f"模型文件未找到: {model_path}",
            error_code="MODEL_NOT_FOUND"
        )


class DatabaseError(SmartTAException):
    """数据库错误"""
    
    def __init__(self, message: str):
        super().__init__(message, error_code="DATABASE_ERROR")


class ConfigurationError(SmartTAException):
    """配置错误"""
    
    def __init__(self, message: str):
        super().__init__(message, error_code="CONFIGURATION_ERROR")


class APIError(SmartTAException):
    """API调用错误"""
    
    def __init__(self, message: str, status_code: Optional[int] = None):
        super().__init__(message, error_code="API_ERROR")
        self.status_code = status_code


class RequestValidationError(SmartTAException):
    """请求验证错误"""
    
    def __init__(self, message: str):
        super().__init__(message, error_code="VALIDATION_ERROR")


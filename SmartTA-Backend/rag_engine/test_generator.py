# test_generator.py
import os
from langchain_openai import ChatOpenAI
from dotenv import load_dotenv

load_dotenv()

def generate_unit_test(requirement: str, context_code: str, class_name: str = "", method_name: str = "") -> str:
    """
    根据用户需求和Java代码生成单元测试
    """
    llm = ChatOpenAI(
        model="deepseek-chat",
        temperature=0.3,  # 降低温度以获得更确定的测试代码
        max_tokens=2048
    )

    prompt = f"""
你是一个专业的Java开发工程师，专门编写高质量的JUnit单元测试。

任务：根据用户的需求描述和提供的Java代码，生成针对性的JUnit 单元测试。

代码上下文：
类名：{class_name}
方法名：{method_name}

用户测试需求：
{requirement}

Java源代码：
```java
{context_code}

请生成完整的JUnit 5测试类，要求：

1.只输出Java测试代码，不要任何解释或markdown标记

2.使用JUnit 5 (Jupiter)

3.包含必要的import语句

4.测试类名格式：{class_name}Test

5.针对用户描述的具体场景编写测试方法

6.使用有意义的测试方法名称，描述测试场景

7.包含必要的断言(assertions)

8.处理边界条件和异常情况

9.使用适当的测试注解(@Test, @BeforeEach等)

10.如果需要，使用Mockito进行mock（但不要过度使用）

生成的测试代码：
"""

    response = llm.invoke(prompt)
    return response.content
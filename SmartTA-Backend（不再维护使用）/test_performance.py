"""
性能测试脚本
测试优化后的API性能
"""
import time
import requests
import statistics


def test_api():
    """测试API性能"""
    base_url = "http://localhost:8000"
    
    # 测试健康检查
    print("=" * 50)
    print("1. 测试健康检查")
    print("=" * 50)
    try:
        response = requests.get(f"{base_url}/health", timeout=5)
        print(f"状态码: {response.status_code}")
        print(f"响应: {response.json()}")
    except Exception as e:
        print(f"❌ 健康检查失败: {e}")
        return
    
    # 测试提问接口
    print("\n" + "=" * 50)
    print("2. 测试提问接口（多次请求以测试缓存效果）")
    print("=" * 50)
    
    test_questions = [
        "什么是操作系统",
        "解释虚拟内存",
        "进程和线程的区别",
    ]
    
    times = []
    success_count = 0
    
    for i, question in enumerate(test_questions * 2, 1):  # 每个问题测试2次
        try:
            start = time.time()
            response = requests.post(
                f"{base_url}/ask",
                json={"question": question, "context_code": ""},
                timeout=120
            )
            elapsed = time.time() - start
            
            if response.status_code == 200:
                success_count += 1
                times.append(elapsed)
                data = response.json()
                answer_length = len(data.get("answer", ""))
                print(f"\n请求 {i}:")
                print(f"  问题: {question}")
                print(f"  响应时间: {elapsed:.2f}秒")
                print(f"  答案长度: {answer_length}字符")
                print(f"  会话ID: {data.get('session_id', 'N/A')}")
            else:
                print(f"请求 {i} 失败: {response.status_code}")
                
        except Exception as e:
            print(f"请求 {i} 异常: {e}")
    
    # 统计结果
    if times:
        print("\n" + "=" * 50)
        print("性能统计")
        print("=" * 50)
        print(f"成功请求: {success_count}/{len(test_questions) * 2}")
        print(f"平均响应时间: {statistics.mean(times):.2f}秒")
        print(f"最快响应: {min(times):.2f}秒")
        print(f"最慢响应: {max(times):.2f}秒")
        print(f"中位数: {statistics.median(times):.2f}秒")
        
        # 优化建议
        print("\n" + "=" * 50)
        print("优化效果")
        print("=" * 50)
        avg_time = statistics.mean(times)
        if avg_time < 1.0:
            print("✅ 优秀！响应时间 < 1秒")
        elif avg_time < 2.0:
            print("✅ 良好！响应时间 < 2秒")
        else:
            print("⚠️  响应时间较长，可能需要检查")


if __name__ == "__main__":
    print("\n🚀 开始性能测试...")
    print("确保后端服务正在运行: uvicorn app:app --reload\n")
    
    test_api()
    
    print("\n✅ 测试完成！")
    print("\n提示：")
    print("- 首次请求会较慢（加载模型）")
    print("- 后续请求应该明显更快（使用缓存）")
    print("- 查看smartta.log获取详细日志")




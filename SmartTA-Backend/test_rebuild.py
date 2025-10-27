"""
测试自动重建功能
"""
import os
import shutil
from rag_engine.model_manager import model_manager
from rag_engine.config import settings

def test_rebuild():
    """测试数据库重建"""
    
    print("=" * 60)
    print("测试自动重建数据库功能")
    print("=" * 60)
    
    # 1. 备份现有数据库
    if os.path.exists(settings.db_path):
        backup_path = settings.db_path + "_backup"
        print(f"\n📦 备份现有数据库到: {backup_path}")
        if os.path.exists(backup_path):
            shutil.rmtree(backup_path)
        shutil.copytree(settings.db_path, backup_path)
        print("✅ 备份完成")
    else:
        print("\n⚠️  数据库不存在，将尝试自动重建")
    
    # 2. 删除现有数据库
    print(f"\n🗑️  删除现有数据库: {settings.db_path}")
    if os.path.exists(settings.db_path):
        shutil.rmtree(settings.db_path)
        print("✅ 删除完成")
    
    # 3. 尝试访问数据库（触发自动重建）
    print("\n🚀 尝试访问数据库（将触发自动重建）...")
    try:
        db = model_manager.get_db()
        print("✅ 数据库访问成功！")
        print(f"   数据库类型: {type(db)}")
        
        # 测试查询
        test_query = "操作系统"
        docs = db.similarity_search(test_query, k=3)
        print(f"\n📚 测试查询: '{test_query}'")
        print(f"   找到 {len(docs)} 个相关文档")
        if docs:
            print(f"   第一个文档长度: {len(docs[0].page_content)} 字符")
        
    except Exception as e:
        print(f"❌ 数据库访问失败: {e}")
        import traceback
        traceback.print_exc()
        
        # 恢复备份
        if os.path.exists(backup_path):
            print(f"\n🔄 恢复备份数据库...")
            shutil.rmtree(settings.db_path)
            shutil.copytree(backup_path, settings.db_path)
            print("✅ 恢复完成")
    
    print("\n" + "=" * 60)
    print("测试完成")
    print("=" * 60)

if __name__ == "__main__":
    test_rebuild()


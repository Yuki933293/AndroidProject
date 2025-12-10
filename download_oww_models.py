import openwakeword
from openwakeword.model import Model


def main():
    # 下载所有预训练唤醒词模型
    openwakeword.utils.download_models()
    # 实例化模型（可根据需要指定具体模型路径）
    Model()


if __name__ == "__main__":
    main()

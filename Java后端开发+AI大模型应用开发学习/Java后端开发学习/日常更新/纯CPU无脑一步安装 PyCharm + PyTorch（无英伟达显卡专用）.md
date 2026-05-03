04.14 19:22
纯CPU无脑一步安装 PyCharm + PyTorch（无英伟达显卡专用）
1.先装 Python（必装第一步）
1.下载地址：https://www.python.org/downloads/windows/
选 Python 3.11.9 下载（PyTorch最稳版本）
2.安装界面左上角一定要勾选 ✅ Add Python to PATH
3.直接点  Install Now  一键安装到底
验证是否装好：
按  Win+R  输入 cmd，回车输入
bash
python --version
 
弹出版本号就成功。
2.安装 PyCharm 免费版
官网：https://www.jetbrains.com/pycharm/download/
选 Community 社区版（完全免费）
下载后一路下一步默认安装即可。
3.一行命令装CPU版PyTorch
重新打开cmd，直接复制粘贴运行：
bash
pip install torch torchvision torchaudio
 
等进度条跑完就装完。
4.PyCharm配置Python解释器
1.打开PyCharm → New Project新建项目
2.Interpreter 选 Existing 已存在解释器
3.浏览找到你安装的  python.exe 
4.等待环境加载完成
5.最终测试代码
新建py文件运行下面这段：
python
import torch
print(torch.__version__)
print(torch.cuda.is_available())
 
不报错、打印版本号就彻底成功
最后一行输出 False 是正常的（你没有N卡GPU）


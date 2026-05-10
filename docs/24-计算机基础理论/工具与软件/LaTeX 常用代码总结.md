03.14 20:01
LaTeX 常用代码总结
LaTeX 是排版学术文档和数学公式的工具，以下按基础语法、公式排版、文档结构、表格图片、常用功能分类整理高频代码，新手可直接复制使用。
 ------------------------------------------------------------------------------------------------
一、 基础文档结构（新建  .tex  文件必备）
latex  
\documentclass{article}  % 文档类型：article（论文）、book（书籍）、report（报告）
\usepackage{ctex}        % 中文支持（需安装相应宏包，如CTeX套装）
\usepackage{amsmath}     % 数学公式核心宏包
\usepackage{graphicx}    % 插入图片宏包
\usepackage{geometry}    % 页面布局宏包
\geometry{a4paper, margin=1in}  % 设置A4纸，页边距1英寸
\begin{document}
% 正文内容写在这里
\end{document}
 
 ------------------------------------------------------------------------------------------------
二、 数学公式排版（最常用）
1. 行内公式（嵌入正文）
用  $ $  或  \( \)  包裹，适合简短公式。
latex  
% 示例
行内公式示例：$E=mc^2$，$\sum_{i=1}^n i = \frac{n(n+1)}{2}$
 
2. 行间公式（独立成行，无编号）
用  \[ \]  或  $$ $$  包裹，适合重要公式。
latex  
% 示例
行间无编号公式：
\[
f(x) = \frac{1}{\sqrt{2\pi}\sigma} e^{-\frac{(x-\mu)^2}{2\sigma^2}}
\]
 
  ------------------------------------------------------------------------------------------------
3. 行间公式（带自动编号）
用  equation  环境，适合需要引用的公式。
latex  
% 示例
\begin{equation}
\label{eq:bayes}  % 公式标签，用于后续引用
P(A|B) = \frac{P(B|A)P(A)}{P(B)}
\end{equation}
引用公式 \eqref{eq:bayes} 即可。
 
4. 常用数学符号代码
符号类型 效果 LaTeX 代码 
希腊字母    \alpha,\beta,\gamma,\pi,\omega  
大写希腊字母    \Gamma,\Delta,\Pi,\Omega  
运算符    \sum,\prod,\int,\lim,\log  
分式    \frac{a}{b}  
根式    \sqrt{a},\sqrt[n]{a}  
下标/上标    a_i,a^2,\sum_{i=1}^n  
向量    \vec{a},\overrightarrow{AB}  
矩阵    \begin{pmatrix}a&b\\c&d\end{pmatrix}  
逻辑符号    \forall,\exists,\in,\notin  
箭头    \Rightarrow,\Leftarrow,\leftrightarrow  
  ------------------------------------------------------------------------------------------------
5. 矩阵排版（ amsmath  宏包）
latex  
% 小矩阵（嵌入正文）
$ \bigl( \begin{smallmatrix} a&b\\c&d \end{smallmatrix} \bigr) $
% 带括号矩阵
\[
\begin{bmatrix} 1&2&3\\4&5&6\\7&8&9 \end{bmatrix}  % 方括号
\begin{vmatrix} 1&2\\3&4 \end{vmatrix}  % 竖线行列式
\]
 
  ------------------------------------------------------------------------------------------------
三、 文档格式与结构
1. 标题、作者、日期
latex  
\title{LaTeX 常用代码总结}
\author{你的名字}
\date{\today}  % 自动生成当前日期，写\date{}则不显示日期
\maketitle  % 生成标题
 
2. 章节与段落
latex  
\section{一级标题}  % 最大级别标题
\subsection{二级标题}
\subsubsection{三级标题}
\paragraph{段落标题}  % 段落级小标题
 
3. 列表环境
- 无序列表
latex  
\begin{itemize}
    \item 第一个项目
    \item 第二个项目
    \item 第三个项目
\end{itemize}
 
- 有序列表
latex  
\begin{enumerate}
    \item 第一步
    \item 第二步
    \item 第三步
\end{enumerate}
 
  ------------------------------------------------------------------------------------------------
四、 表格与图片
1. 表格（ tabular  环境）
latex  
% 三线表（常用学术表格）
\usepackage{booktabs}  % 导入三线表宏包
\begin{table}[h]  % [h]表示表格放在当前位置
    \centering  % 表格居中
    \caption{表格标题}  % 表格标题
    \begin{tabular}{c|cc}  % c:居中对齐，|：竖线
        \toprule  % 顶线
        表头1 & 表头2 & 表头3 \\
        \midrule  % 中线
        内容1 & 内容2 & 内容3 \\
        内容4 & 内容5 & 内容6 \\
        \bottomrule  % 底线
    \end{tabular}
\end{table}
 
2. 插入图片（ graphicx  宏包）
latex  
\begin{figure}[h]
    \centering  % 图片居中
    \includegraphics[width=0.5\textwidth]{image.jpg}  % 宽度设为页面50%
    \caption{图片标题}  % 图片标题
    \label{fig:myimage}  % 图片标签，用于引用
\end{figure}
引用图片 \ref{fig:myimage} 即可。
 
  ------------------------------------------------------------------------------------------------
五、 常用高级功能
1. 参考文献（ bibtex  基础）
1. 新建  .bib  文件（如  ref.bib ），添加文献条目：
bibtex  
@article{einstein1905,
    title={On the electrodynamics of moving bodies},
    author={Einstein, A.},
    journal={Annalen der Physik},
    volume={17},
    pages={891--921},
    year={1905}
}
 
2. 在  .tex  文件中引用并生成参考文献：
latex  
\cite{einstein1905}  % 引用文献
\bibliographystyle{plain}  % 参考文献格式
\bibliography{ref}  % 导入.bib文件
 
2. 代码块排版（ listings  宏包）
latex  
\usepackage{listings}
\usepackage{xcolor}  % 代码高亮颜色
\lstset{
    language=Java,  % 代码语言：Java/Python/C++
    basicstyle=\small\ttfamily,  % 字体
    keywordstyle=\color{blue},  % 关键字颜色
    commentstyle=\color{gray},  % 注释颜色
    numbers=left,  % 显示行号
    breaklines=true  % 自动换行
}
\begin{lstlisting}
// Java 代码示例
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello LaTeX!");
    }
}
\end{lstlisting}
  ------------------------------------------------------------------------------------------------
六、 常见问题解决
1. 中文乱码：导入  ctex  宏包，文档保存为 UTF-8 编码，使用 CTeX 套装编译。
2. 公式编译错误：确保导入  amsmath  宏包，符号语法正确（如成对的大括号）。
3. 图片无法显示：检查图片路径是否正确，格式支持  jpg/png/pdf 。


// @ts-ignore
const fs = require("fs");
// @ts-ignore
const path = require("path");

// 定义生成 README.md 文件的函数
function genReadme(directory) {
  if (!fs.existsSync(directory)) {
    console.error(`错误：目录 "${directory}" 不存在`);
    process.exit(1);
  }

  const dirName = path.basename(directory);
  const readmeContent = generateContent(directory, dirName);
  const readmePath = path.join(directory, "README.md");

  // 写入 README.md 文件
  fs.writeFileSync(readmePath, readmeContent);
  console.log(`README.md 文件已生成在 ${readmePath}，⚠️注意检查是否有问题！`);
}

// 递归生成 Markdown 内容
function generateContent(directory, dirName) {
  let content = `# ${dirName}\n\n`;
  content += `> 你全面的 AI 知识库，一网打尽最新 AI 资讯，都在 [https://ai.codefather.cn](https://ai.codefather.cn)\n\n`;

  // 获取所有一级子目录并按创建时间排序，最新的放在前面
  const subDirs = getSubDirectories(directory).sort((a, b) => {
    const statA = fs.statSync(a);
    const statB = fs.statSync(b);
    return statB.birthtime.getTime() - statA.birthtime.getTime();
  });

  if (subDirs.length > 0) {
    // 循环处理每个一级子目录
    for (const subDir of subDirs) {
      const subDirName = path.basename(subDir);

      content += `## ${subDirName}\n\n`;

      // 递归获取子目录下的所有 Markdown 文件
      const subDirFiles = getFilesInDirectory(subDir).sort((a, b) => {
        // First check for DeepSeek guide
        const nameA = path.basename(a);
        const nameB = path.basename(b);
        if (nameA.includes("🔥DeepSeek 小白快速上手指南")) return -1;
        if (nameB.includes("🔥DeepSeek 小白快速上手指南")) return 1;

        // Then sort by date for other files
        const statA = fs.statSync(a);
        const statB = fs.statSync(b);
        return statB.birthtime.getTime() - statA.birthtime.getTime();
      });

      for (let i = 0; i < Math.min(subDirFiles.length, 100); i++) {
        const file = subDirFiles[i];

        // 跳过 README.md 文件
        if (path.basename(file).toLowerCase() === "readme.md") {
          continue;
        }

        const relativePath = path.relative(directory, file)?.replaceAll(" ", "%20");
        content += `[${path.basename(file, ".md")}](${relativePath})\n\n`;
      }
    }
  } else {
    // 如果没有子目录，直接处理当前目录下的 Markdown 文件
    const files = getFilesInDirectory(directory).sort((a, b) => {
      const statA = fs.statSync(a);
      const statB = fs.statSync(b);
      return statB.birthtime.getTime() - statA.birthtime.getTime();
    });

    for (let i = 0; i < Math.min(files.length, 100); i++) {
      const file = files[i];
      if (path.basename(file).toLowerCase() === "readme.md") continue;
      const relativePath = path.basename(file)?.replaceAll(" ", "%20");
      content += `[${path.basename(file, ".md")}](${relativePath})\n\n`;
    }
  }
  if (subDirs.length > 0) {
    // 添加底部内容
    content += `> 你全面的 AI 知识库，一网打尽最新 AI 资讯，都在 [https://ai.codefather.cn](https://ai.codefather.cn)\n\n`;
  }

  return content;
}

// 获取目录下的所有子目录
function getSubDirectories(directory) {
  const items = fs.readdirSync(directory, { withFileTypes: true });
  return items.filter((item) => item.isDirectory()).map((dir) => path.join(directory, dir.name));
}

// 递归获取目录下的所有 Markdown 文件
function getFilesInDirectory(directory) {
  const items = fs.readdirSync(directory, { withFileTypes: true });
  let files = [];

  for (const item of items) {
    const fullPath = path.join(directory, item.name);
    if (item.isDirectory()) {
      // 递归获取子目录中的文件
      files = files.concat(getFilesInDirectory(fullPath));
    } else if (item.isFile() && path.extname(item.name) === ".md") {
      files.push(fullPath);
    }
  }

  return files;
}

// 从命令行参数获取目标目录
const targetDirectory = process.argv[2];
if (!targetDirectory) {
  console.error("错误：请提供目标目录路径作为参数");
  process.exit(1);
}

// 调用生成 README.md 文件的函数
genReadme(targetDirectory);

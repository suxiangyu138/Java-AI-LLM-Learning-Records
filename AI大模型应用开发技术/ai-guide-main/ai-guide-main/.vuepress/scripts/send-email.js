const nodemailer = require("nodemailer");

// 从命令行参数获取邮箱配置
const [emailUser, emailPass, toEmail] = process.argv.slice(2);
const repoName = process.env.GITHUB_REPOSITORY || "未知仓库";
const runId = process.env.GITHUB_RUN_ID || "未知";
const runUrl = `https://github.com/${repoName}/actions/runs/${runId}`;
const branch = process.env.GITHUB_REF_NAME || "main";

async function sendEmail() {
  // 创建邮件传输器
  const transporter = nodemailer.createTransport({
    service: "qq", // 或其他服务，如 'gmail', '163' 等
    auth: {
      user: emailUser,
      pass: emailPass, // QQ 邮箱需要使用授权码而非密码
    },
  });

  // 设置邮件内容
  const mailOptions = {
    from: emailUser,
    to: toEmail,
    subject: `【构建通知】AI 知识库已成功部署 - ${new Date().toLocaleString()}`,
    html: `
      <div style="font-family: Arial, sans-serif; padding: 20px; max-width: 600px; margin: 0 auto; border: 1px solid #eee; border-radius: 5px;">
        <h2 style="color: #18b566;">✅ 部署成功通知</h2>
        <p>您的 <strong>AI 知识库</strong> 网站已成功构建并部署到腾讯云 COS！</p>
        <ul style="list-style-type: none; padding-left: 0;">
          <li><strong>仓库:</strong> ${repoName}</li>
          <li><strong>分支:</strong> ${branch}</li>
          <li><strong>部署时间:</strong> ${new Date().toLocaleString()}</li>
        </ul>
        <p>
          <a href="${runUrl}" style="background-color: #1a73e8; color: white; padding: 10px 20px; text-decoration: none; border-radius: 4px; display: inline-block; margin-top: 10px;">
            查看构建详情
          </a>
        </p>
        <p style="color: #666; font-size: 0.9em; margin-top: 20px;">
          此邮件由 GitHub Actions 自动发送，请勿回复。
        </p>
      </div>
    `,
  };

  try {
    // 发送邮件
    const info = await transporter.sendMail(mailOptions);
    console.log("邮件发送成功：", info.messageId);
    return true;
  } catch (error) {
    console.error("邮件发送失败：", error);
    return false;
  }
}

// 执行邮件发送
sendEmail()
  .then((success) => process.exit(success ? 0 : 1))
  .catch((err) => {
    console.error(err);
    process.exit(1);
  });

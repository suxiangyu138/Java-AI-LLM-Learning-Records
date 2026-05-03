# Vibe Coding Security Tips

> Protect Your Projects and API Keys



Hello, I'm Yupi.

Many beginners who use AI for projects completely overlook security issues. As long as the code runs, they don't care about security, thinking they'll deal with it when problems arise. However, a security issue can destroy an entire project.

I've seen someone get charged thousands overnight due to an API Key leak. I've also seen someone's database get deleted, losing all user data. As for projects from big companies, even a minor issue can cause a huge uproar.

In this article, I'll discuss the most commonly overlooked security issues in Vibe Coding and how to avoid them.




## 1. The Deadly Issue - API Key Leak

Among all security issues, API Key leaks are one of the most common and deadly.



### What is an API Key?

An API Key is like the key to your house. With it, you can access a service. For example, OpenAI's API Key allows you to call ChatGPT, and Supabase's API Key lets you access a database.

The problem is, if someone else gets this key, they can impersonate you and use these services. If it's a paid service, they'll spend your money; if it's a database, they might read, modify, or even delete your data.




### Common Ways of Leakage

The most common way API Keys leak is by **writing them directly in the code and uploading it to GitHub**.

Many beginners might write code to call an AI model like this:

```typescript
// ❌ Never do this!
const OPENAI_API_KEY = 'sb-yupi-abc123def456...';

const response = await fetch('https://xxx/v1/chat/completions', {
  headers: {
    'Authorization': `Bearer ${OPENAI_API_KEY}`
  }
});
```

And then push the code to GitHub.

What happens next?

Since your GitHub project is public, anyone can see your code and, consequently, your API Key. Worse, there are automated scripts specifically scanning GitHub for API Keys, and once found, they'll use them immediately.

I heard of someone who directly wrote OpenAI's API Key in their frontend code. Someone found it in the browser's developer tools, and within hours, they were charged thousands. By the time he noticed, the money was already gone.

This lesson tells us: **API Key leaks are not trivial; they must be taken seriously.**




## 2. How to Properly Manage Sensitive Information?

If you shouldn't write API Keys in the code, what should you do?



### Use Environment Variables

The correct approach is to use environment variables.

Environment variables are configuration information stored in the system or runtime environment and are not included in the code.

#### Step 1: Create a .env File

Create a `.env` file in the project root directory:

```
OPENAI_API_KEY=sb-yupi-abc123def456...
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_ANON_KEY=eyJhbGci...
DATABASE_URL=postgresql://...
```



#### Step 2: Use Them in Code

Access these variables in your code via `process.env`:

```typescript
// ✅ Correct approach
const OPENAI_API_KEY = process.env.OPENAI_API_KEY;

const response = await fetch('https://xxx/v1/chat/completions', {
  headers: {
    'Authorization': `Bearer ${OPENAI_API_KEY}`
  }
});
```



#### Step 3: Add to .gitignore

Ensure the `.env` file is not uploaded to GitHub:

```
# .gitignore
.env
.env.local
.env.*.local
```



#### Step 4: Create .env.example

To let others know which environment variables are needed, create a `.env.example` file:

```
OPENAI_API_KEY=your_openai_api_key_here
SUPABASE_URL=your_supabase_url_here
SUPABASE_ANON_KEY=your_supabase_key_here
DATABASE_URL=your_database_url_here
```

This file can be uploaded to GitHub as it doesn't contain real keys.

### Frontend vs Backend

There's an important distinction: **Frontend code is public, backend code is private.**

In the frontend (code running in the browser), even if you use environment variables, these values will eventually be bundled into JavaScript files, and users can see them via developer tools. So, **never use sensitive API Keys in frontend code!**

The correct approach is:

- Place sensitive API calls in the backend
- Frontend only calls your own backend API
- Backend verifies user identity before calling third-party APIs

Here are some code examples:

```typescript
// ❌ Don't call OpenAI directly in the frontend
// Frontend code
const response = await fetch('https://api.openai.com/v1/chat/completions', {
  headers: { 'Authorization': `Bearer ${OPENAI_API_KEY}` }
});

// ✅ Correct approach
// Frontend code: Call your own backend
const response = await fetch('/api/chat', {
  method: 'POST',
  body: JSON.stringify({ message: userMessage })
});

// Backend code: Call OpenAI
export async function POST(request: Request) {
  const { message } = await request.json();
  
  // Use API Key in the backend
  const response = await fetch('https://api.openai.com/v1/chat/completions', {
    headers: { 'Authorization': `Bearer ${process.env.OPENAI_API_KEY}` },
    body: JSON.stringify({ messages: [{ role: 'user', content: message }] })
  });
  
  return response;
}
```



### Use Secret Management Services

For production environments, it's recommended to use specialized secret management services like Vercel's environment variable management, AWS Secrets Manager, HashiCorp Vault, etc. These services provide more secure key storage and access control, which is what big companies typically do.




## 3. Other Common Security Issues

Besides API Key leaks, there are other common security issues.



### SQL Injection

SQL injection is one of the most classic security vulnerabilities. If you directly concatenate user input into SQL queries, attackers can execute malicious SQL statements through specially crafted input.

```typescript
// ❌ Dangerous: SQL injection risk
const query = `SELECT * FROM users WHERE email = '${userEmail}'`;

// ✅ Safe: Use parameterized queries
const query = 'SELECT * FROM users WHERE email = ?';
const result = await db.execute(query, [userEmail]);
```

Fortunately, if you use modern tools like Supabase or Prisma, they automatically prevent SQL injection. But if you write raw SQL, be cautious of this issue.



### XSS Attacks

XSS (Cross-Site Scripting) refers to attackers injecting malicious scripts into your website.

For example, if you directly display user input on the page:

```typescript
// ❌ Dangerous: XSS risk
function Comment({ text }) {
  return <div dangerouslySetInnerHTML={{ __html: text }} />;
}
```

An attacker could input `<script>alert('Yupi is a dog')</script>`, and this script would execute in other users' browsers.

![](https://pic.yupi.icu/1/image-20260105155517608.png)

The correct approach is:

```typescript
// ✅ Safe: React automatically escapes
function Comment({ text }) {
  return <div>{text}</div>;
}
```

React automatically escapes all content unless you use `dangerouslySetInnerHTML`. So, **avoid using `dangerouslySetInnerHTML` unless absolutely necessary.**



### CSRF Attacks

CSRF (Cross-Site Request Forgery) refers to attackers tricking users into performing unintended actions on a logged-in website.

For example, if you're logged into a bank website and open a malicious site in another tab, the malicious site could send a transfer request to the bank website. Since you're still logged in, the bank website would think it's your action and execute the transfer. This is a CSRF attack.

There are three common methods to defend against CSRF:

1) Use CSRF Tokens: The server generates a random token, and each form submission must include this token. The server verifies the token's correctness.

2) Use SameSite Cookie Attribute: Set the SameSite attribute on cookies so the browser only sends cookies in same-site requests.

3) Verify the Referer Header: Check which website the request originated from and reject it if it's not your own site.

If you use modern frameworks like Next.js or Nuxt.js, they usually handle CSRF protection automatically.




### Authentication and Authorization

Never trust any validation on the frontend!

**Frontend validation is only for user experience; real validation must be done on the backend.**

Here are some code examples:

```typescript
// ❌ Insecure: Only checks on the frontend
function AdminPanel() {
  const isAdmin = localStorage.getItem('isAdmin') === 'true';
  if (!isAdmin) return <div>无权访问</div>;
  return <div>管理面板</div>;
}

// ✅ Secure: Validate on the backend
// Frontend
function AdminPanel() {
  const { data, error } = useFetch('/api/admin/data');
  if (error) return <div>无权访问</div>;
  return <div>管理面板</div>;
}

// Backend
export async function GET(request: Request) {
  const user = await verifyToken(request);
  if (!user.isAdmin) {
    return new Response('Forbidden', { status: 403 });
  }
  // Return data
}
```



### Dependency Security

If your project uses many third-party packages, these packages might also have security vulnerabilities.

When the city gate catches fire, it affects the fish in the moat. It's recommended to regularly run `npm audit` to check for vulnerabilities.

If vulnerabilities are found, run the following command, which will automatically update vulnerable packages.

```bash
npm audit fix
```

For vulnerabilities that can't be automatically fixed, manually inspect and decide if the package needs to be replaced.




## 4. Security Checklist

Before releasing a project, it's recommended to go through this checklist with both AI and human review.




### Keys and Sensitive Information

- [ ] All API Keys use environment variables
- [ ] .env file is added to .gitignore
- [ ] .env.example file is created
- [ ] Sensitive API calls are made in the backend
- [ ] No keys are exposed in frontend code
- [ ] Production environment keys are different from development keys


### User Input Validation

- [ ] All user input is validated
- [ ] Both frontend and backend validate input
- [ ] Parameterized queries are used to prevent SQL injection
- [ ] Avoid using dangerouslySetInnerHTML
- [ ] Check the type and size of user-uploaded files


### Authentication and Authorization

- [ ] Secure authentication schemes are used (e.g., JWT, OAuth)
- [ ] Passwords are stored encrypted (using bcrypt, etc.)
- [ ] Sensitive operations require re-authentication
- [ ] Permission control is implemented; different users have different permissions
- [ ] Sessions have expiration times


### HTTPS and Transmission Security

- [ ] Production environment uses HTTPS
- [ ] Cookies have Secure and HttpOnly flags set
- [ ] SameSite Cookies are used to prevent CSRF
- [ ] Sensitive data is encrypted during transmission


### Dependencies and Third-Party Services

- [ ] Regularly run npm audit to check for vulnerabilities
- [ ] Only use trusted third-party packages
- [ ] Regularly update dependencies
- [ ] Review permissions of third-party packages


### Error Handling and Logging

- [ ] Error messages don't expose sensitive information
- [ ] Detailed error stacks aren't shown in production
- [ ] Logs don't record sensitive information like passwords
- [ ] Error monitoring and alerts are implemented



## 5. Let AI Help with Security Checks

Undoubtedly, AI can also help you discover and fix security issues.

You can ask AI to review your code for security:

```markdown
Please review this code from a security perspective and identify potential security issues:
【Paste your code here】
Focus on:
1. Is there SQL injection risk?
2. Is there XSS attack risk?
3. Is user input validated?
4. Is sensitive information exposed?
5. Is error handling secure?
```

AI will give you a detailed security analysis.

After identifying issues, ask AI to help fix them:

```markdown
You mentioned this code has SQL injection risk. Please provide a secure implementation using parameterized queries.
The user input here isn't validated. Please add validation logic to ensure email format is correct and password is at least 8 characters long.
```

But remember, **don't completely rely on AI's security advice**. AI might miss some issues or provide less secure solutions. Use your judgment, consult official documentation, or confirm with multiple AI models if necessary.




### Use Security Scanning Tools

Besides AI, you can also use specialized security scanning tools:

- Snyk: Scans dependency vulnerabilities
- SonarQube: Code quality and security analysis
- OWASP ZAP: Web application security testing

![](https://pic.yupi.icu/1/evo-by-snyk-hp-image_j0acql.png)

These tools can automatically discover many security issues.




## 6. Habits for Secure Development

Security is not a one-time task but a habit that must be cultivated and always kept in mind.



### Principle of Least Privilege

Assign only the necessary permissions to each user and service, no more.

For example, if an API Key only needs to read data, don't give it write permissions. If a user is just a regular user, don't give them admin privileges.

This way, even if a key or account is compromised, the damage will be limited.




### Regularly Rotate Keys

Don't use the same API Key forever. Rotate keys regularly, such as every 3 or 6 months.

Most services support creating multiple API Keys. You can create a new key, update it in your project, confirm everything works, and then delete the old key.




### Monitor Abnormal Activities

Many API services provide usage monitoring and alerting features. Remember to enable them to detect anomalies promptly.

If your API call volume suddenly spikes, it might be due to key misuse. If someone fails multiple login attempts, it could be a brute-force attack.




### Keep Updated

Security vulnerabilities are constantly discovered, and software packages are continuously updated to fix them. Regularly update your dependencies, follow security bulletins, and promptly fix known security issues.




### Backup Data

Even with all protections in place, things can still go wrong. It's recommended to regularly back up your data, so you can recover even in the worst-case scenario.

If you use third-party backend services like Supabase, they might automatically back up data. If it's your own database, set up regular backups.




## Final Words

Security issues are often the most overlooked because they aren't as intuitive as functionality or performance. But once a security issue arises, the consequences can be catastrophic.

To summarize the key points of this article:

1. API Key leaks are the biggest risk: Never write API Keys in the code; use environment variables.
2. Distinguish between frontend and backend: Sensitive operations must be done in the backend; never trust frontend validation.
3. Understand common vulnerabilities: SQL injection, XSS, CSRF, etc., must be guarded against.
4. Use a security checklist: Go through the checklist before each release to ensure nothing is missed.
5. Cultivate security habits: Least privilege, regular key rotation, monitor anomalies, keep updated, backup data.
6. Leverage tools: AI and security scanning tools can help discover issues, but don't rely on them completely.

Security is an ongoing process, not a one-time task. Stay vigilant, regularly check, and protect your projects and users.

I hope these security tips help you avoid common security issues and make your Vibe Coding projects more secure and reliable.

You've worked hard learning, treat yourself to a chicken leg 🍗, and then get started!




## Recommended Resources

1) Yupi AI Navigation Website: [AI Resource Collection, Latest AI News, Free AI Tutorials](https://ai.codefather.cn)

2) Programming Navigation Learning Circle: [Learning Paths, Programming Tutorials, Practical Projects, Job Hunting Guide, Q&A](https://www.codefather.cn)

3) Programmer Interview Cheat Sheet: [Internship/Campus/Job Hunting High-Frequency Topics, Enterprise Exam Analysis](https://www.mianshiya.com)

4) Programmer Resume Writing Tool: [Professional Templates, Rich Examples, Direct to Interview](https://www.laoyujianli.com)

5) 1-on-1 Mock Interview: [Essential for Internship/Campus/Job Hunting Interviews to Get Offers](https://ai.mianshiya.com)
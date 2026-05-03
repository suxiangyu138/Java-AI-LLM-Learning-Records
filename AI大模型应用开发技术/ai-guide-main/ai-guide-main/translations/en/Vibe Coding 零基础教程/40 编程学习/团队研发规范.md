# Team Development Standards

> Enterprise-Level Development Standards Practice Guide

Hello, I am programmer Yupi.

A few days ago, I shared a retrospective summary of my one-year entrepreneurial journey, mentioning that as the team grows, we will pay more attention to development standards and technical accumulation.

Some programmer friends asked: What are development standards?

Others said: Yupi, don't treat us as outsiders, share your company's development standards with us!

![Development Standards](https://pic.yupi.icu/1/image-20240419140208921.png)

Sure, I must arrange it!

This article will briefly share our company's development standards. However, before we start, two points must be clarified:

1. Each team should customize their own development standards based on their situation. Others' standards are for reference only and may not be the best fit for your team.
2. Due to limited space, this article only shares some standards I consider important, and we have removed our own sensitive information.

⭐️ Video version of this article: https://bilibili.com/video/BV1fi421C78M

## I. Overall Project Development Process

1) Team Confirms Goals and Plans

Hold meetings to discuss and produce goal and planning documents.

2) Product Research and Requirement Analysis

Produce research reports and requirement analysis documents.

3) Requirement Review

Hold requirement review meetings to clarify the requirements and tasks, estimate workload, and define work timelines.

4) Solution Design

Produce solution design documents, such as database table design, page design, interface design, etc.

5) Development

Includes individual development, unit testing, front-end and back-end integration, etc.

6) Testing and Acceptance

Includes self-testing by developers, product acceptance, and team acceptance.

7) Code Submission

Submit code ready for deployment, which needs to be reviewed by the responsible person and can be merged upon approval.

8) Deployment and Launch

Deploy the code to the server, notify the team, update the deployment documentation, and verify the deployment after launch.

9) Product Iteration

Continuously collect user feedback on new features, perform data analysis to validate the changes, and facilitate the next round of updates and iterations.

## II. Development Standards

### Pre-Development Considerations

1) Ensure a thorough understanding of the business and requirements; conduct an overall solution design first. Especially for important requirements and core business, confirm the solution with team members before starting development to avoid redundant work.

2) Familiarize yourself with the project before development. It is recommended to read project documentation, project code, interface documentation, front-end component documentation, etc.

3) Be cautious when introducing new dependencies or libraries, or upgrading versions. Major dependency changes need to be confirmed with other team members.

4) Familiarize yourself with the functionalities and code already implemented by the team, and try to reuse them to avoid redundant development.

5) Familiarize yourself with the team's internal development standards and configure them in the IDE, such as setting up ESLint and Prettier plugins for front-end code standardization.

### During Development Considerations

1) When developing new features, ensure you pull the **latest main branch** code from the project repository.

2) Create a new branch for each feature development. **Never modify the main branch directly!** Ensure the branch name is in English, semantically meaningful, and not confused with others.

3) During development, try to reuse existing functionalities, modules, classes, methods, and object code. If existing code is available, avoid rewriting it.

4) Follow the team's internal development standards during development. Refer to the existing project code's style, especially avoiding inconsistent formatting, naming, and writing styles with the original project.

5) During development, if anything is unclear, do not guess. Contact other project members or the responsible person promptly for confirmation.

6) During development, periodically (e.g., every 1 - 3 days) use `git pull` to synchronize with the latest main branch code to prevent merge conflicts.

7) During development, pay attention to the overall timeline. Complete first, perfect later. Provide timely feedback if there are risks.

8) During development, pay special attention to capturing and handling exceptions.

9) Keep each branch as clean as possible. Minimize the amount of code changed during each development and submission. It is recommended to modify only one feature, bug, or module per branch. Avoid combining unrelated functionalities and make changes only when necessary.

10) After completing part of the feature development, always conduct self-testing! Mock fake data during self-testing. **Never test on the production environment and never affect production data!**

## III. Code Submission Standards

1) Only code that has passed testing and product acceptance can initiate a PR request to merge into the main branch. Before that, it can be submitted to your own branch.

2) Before initiating a PR to merge into the main branch, **read your own code thoroughly three times** to avoid non-standard writing and meaningless changes.

3) Each merge should focus on one feature or change, avoiding coupling multiple functionalities together, improving review efficiency, and reducing change risks.

4) Each submission should include a description of the code changes in the commit message. Additional explanations can be provided by linking requirement documents, test cases, solution documents, and effect screenshots.

Commit messages can refer to the [Conventional Commits](https://www.conventionalcommits.org/zh-hans/v1.0.0/) document, but it is not mandatory.

5) Unless under special circumstances, all code must be reviewed and approved by at least one project lead before merging. Only code merged into the main branch is allowed to be deployed.

## IV. Deployment Standards

### Pre-Deployment Considerations

1) Before deployment, in addition to verifying that the functionality runs correctly and meets requirements, pay special attention to the program's:

- Robustness. For example, providing user-friendly error messages and input validation.
- Security. Preventing unauthorized operations and input validation.
- Stability. Ensuring calls are 100% successful. If there is a chance of failure, consider retry or fault-tolerant strategies.

2) Unless under special circumstances, only functionalities verified by the product and main branch code that has passed code review are allowed to be deployed.

3) Unless under special circumstances, try to deploy during weekdays (recommended Tuesday ~ Thursday) to ensure timely fixes if issues arise after deployment.

### Post-Deployment Considerations

1) After deployment, always conduct a complete testing process again, especially focusing on permission-related functionality tests.

2) After deployment, always promptly share deployment information in the group, notify relevant members, and provide immediate feedback if issues are encountered.

3) After the first deployment, configure monitoring alerts immediately.

4) After deployment verification passes and is confirmed by internal group members, announce the version update in external user groups.

5) After deployment, update the project's update record documentation immediately.

6) Note that deployment is not the end. For a period after deployment (at least one week), continuously monitor the functionality you are responsible for, collect user feedback, observe the new feature's effectiveness through data analysis, and fix any issues promptly, preparing for the next improvement iteration.

## Final Words

The above are our company's development standards, hoping they are helpful to you.

Again, each team should customize their development standards based on their situation. These standards are for reference only.

## Recommended Resources

1) Yupi AI Navigation Website: [AI Resource Collection, Latest AI News, Free AI Tutorials](https://ai.codefather.cn)

2) Programming Navigation Learning Circle: [Learning Paths, Programming Tutorials, Practical Projects, Job Hunting Guide, Q&A](https://www.codefather.cn)

3) Programmer Interview Cheat Sheet: [Internship/Campus Recruitment/Social Recruitment High-Frequency Exam Points, Enterprise Real Questions Analysis](https://www.mianshiya.com)

4) Programmer Resume Writing Tool: [Professional Templates, Rich Examples, Direct to Interview](https://www.laoyujianli.com)

5) 1-on-1 Mock Interview: [Essential for Internship/Campus Recruitment/Social Recruitment Interviews to Get Offers](https://ai.mianshiya.com)
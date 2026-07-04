# Privacy & Data Security

At **Ferry**, we believe that an AI agent with access to your device's screen must be built with absolute transparency and zero compromises on privacy. 

Because Ferry is an on-device agent, it reads your screen to perform tasks. We have designed the architecture specifically to ensure that you, and only you, have control over where that data goes.

## 1. Zero Telemetry & No Middleman Servers
**Ferry operates entirely without a backend.** We do not operate any servers that your data passes through. We do not use analytics SDKs (no Firebase, no Google Analytics, no Crashlytics). 

## 2. Where Your Data Goes
The only network calls this app makes are directly to **your chosen LLM provider's official API** (e.g., `api.anthropic.com` or `api.openai.com`), using the API key you provide.

When the agent takes a step, it sends the following directly to your configured provider:
- **Your task description** (e.g., "Summarize my unread emails")
- **The current screen's accessibility tree**: This is the structural text of whatever is visible on your screen at that exact moment. **Note: This can include sensitive information if you run the agent while viewing personal emails, passwords, or messages.**
- **Action History**: A log of what the agent has clicked or typed so far during the current task.

## 3. How Your API Keys Are Protected (BYOK)
You bring your own key (BYOK). Your API key is stored locally on your device using Android's hardware-backed **EncryptedSharedPreferences** (the Android Keystore system). 
- It is never logged.
- It is never included in crash reports.
- It is never sent anywhere except as the standard `Authorization` header directly to the LLM provider.

## 4. Local Task History
Your task execution history (the timeline of what the agent did) is stored purely locally on your device using a Room Database. It never leaves your phone.

## How to use Ferry safely
Because the accessibility tree (the text on your screen) is sent to your LLM provider as part of normal operation, **you are trusting your chosen LLM provider (e.g., Anthropic, OpenAI) with that data.** 

- **Do not run tasks on screens showing highly sensitive information** (like password managers or banking apps) unless you explicitly trust your configured LLM provider's data retention policies.
- **Review your provider's privacy policy.** For example, Anthropic and OpenAI generally state that they do not use data submitted via their APIs to train their models, and they retain data for a maximum of 30 days for abuse monitoring, but you should verify this yourself.
- If you require strict zero-data-sharing guarantees, wait for our upcoming **Local Model Support**, which will allow you to run smaller, quantized LLMs entirely on your phone's hardware.

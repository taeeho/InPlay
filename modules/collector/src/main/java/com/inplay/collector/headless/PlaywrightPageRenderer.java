package com.inplay.collector.headless;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import java.net.URI;
import java.util.Objects;

public final class PlaywrightPageRenderer implements PageRenderer, AutoCloseable {

    private final Playwright playwright;
    private final Browser browser;

    public PlaywrightPageRenderer() {
        this(Playwright.create());
    }

    PlaywrightPageRenderer(Playwright playwright) {
        this.playwright = Objects.requireNonNull(playwright, "playwright required");
        this.browser = this.playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @Override
    public String render(URI url, RenderOptions options) {
        Objects.requireNonNull(url, "url required");
        Objects.requireNonNull(options, "options required");
        try (BrowserContext context = browser.newContext(
                new Browser.NewContextOptions().setUserAgent(options.userAgent()))) {
            for (String pattern : options.abortPatterns()) {
                context.route(pattern, route -> route.abort());
            }
            Page page = context.newPage();
            page.setDefaultTimeout(options.timeout().toMillis());
            page.navigate(url.toString(),
                    new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            if (options.waitForSelector() != null && !options.waitForSelector().isEmpty()) {
                page.waitForSelector(options.waitForSelector());
            }
            return page.content();
        }
    }

    @Override
    public void close() {
        try {
            browser.close();
        } finally {
            playwright.close();
        }
    }
}

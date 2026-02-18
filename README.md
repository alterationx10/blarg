# Blarg

A simple static site generator for Markdown content, built with Scala 3.

Blarg transforms your Markdown files into a complete static website with blog support, tags, templating, and built-in
link validation. Perfect for personal sites, blogs, documentation, and project pages.

## Features

- **Markdown-First**: Write content in Markdown with extensive CommonMark extension support
- **Blog-Ready**: Built-in blog post management with date-based URLs and latest posts page
- **Smart Organization**: Automatic tag pages and chronological post ordering
- **Template System**: Mustache templates with partials for flexible layouts
- **Link Validation**: Catches broken internal links before deployment
- **Live Development**: Watch mode rebuilds on file changes
- **Built-in Server**: Serve your site locally for testing

### Markdown Extensions

Blarg supports these CommonMark extensions out of the box:

- **Autolink** - Automatic URL linking
- **Strikethrough** - `~~strikethrough~~` text
- **Tables** - GitHub-flavored tables
- **Footnotes** - Reference-style footnotes
- **Heading Anchors** - Automatic ID generation for headings
- **Ins** - `++inserted++` text
- **YAML Frontmatter** - Metadata for pages and posts
- **Image Attributes** - Width, height, and other image properties

## Installation

You can check the [GitHub releases](https://github.com/alterationx10/blarg/releases) page for pre-built linux-x64
versions (should also work on Mac). If you want to build from source, read on.

### Prerequisites

- Java 25 or later
- Scala CLI (for building from source)

### Building from Source

```bash
git clone https://github.com/alterationx10/blarg.git
cd blarg
scala-cli --power package . -o blarg -f
```

This creates a standalone executable `blarg` that you can move to your PATH.

## Quick Start

### 1. Create a New Site

```bash
blarg new my-blog
cd my-blog
```

This creates the following structure:

```
my-blog/
├── site/
│   ├── blarg.json          # Site configuration
│   ├── blog/               # Blog posts
│   │   └── YYYY-MM-DD-first-post.md
│   ├── pages/              # Static pages
│   │   ├── index.md
│   │   └── about.md
│   ├── static/             # CSS, JS, images
│   │   ├── css/
│   │   ├── js/
│   │   └── img/
│   └── templates/          # Mustache templates
│       ├── site.mustache
│       ├── pages/
│       │   ├── page.mustache
│       │   ├── blog.mustache
│       │   ├── tags.mustache
│       │   └── latest.mustache
│       └── partials/
│           ├── header.mustache
│           ├── nav.mustache
│           └── footer.mustache
└── .gitignore
```

### 2. Build Your Site

```bash
blarg build -d ./site
```

The generated site appears in `./build/` (sibling to the `site` folder).

### 3. Serve Locally

```bash
blarg serve
```

Visit http://localhost:9000 to preview your site.

### 4. Development with Watch Mode

```bash
blarg build -d ./site --watch
```

Blarg will rebuild automatically when files change. Press Return to stop.

## Commands

### `blarg new <name>`

Create a new site with the default template structure.

**Options:**

- `-d, --dir <path>` - Parent directory for the new site (default: `.`)

**Examples:**

```bash
blarg new my-blog
blarg new my-blog -d ~/projects
```

### `blarg build`

Build the static site from Markdown files.

**Options:**

- `-d, --dir <path>` - Path to site folder (default: `./site`)
- `-w, --watch` - Watch for changes and rebuild automatically

**Examples:**

```bash
blarg build
blarg build -d ./site
blarg build -d ./site --watch
```

**Output:**

- Generated files go to `../build/` relative to the site folder
- Blog posts: `/YYYY/MM/DD/slug.html`
- Pages: `/page-name.html`
- Special pages: `/tags.html`, `/latest.html`

### `blarg serve`

Start a local HTTP server to preview your site.

**Options:**

- `-d, --dir <path>` - Directory to serve (default: `./build`)
- `-p, --port <number>` - Port number (default: `9000`)
- `--no-tty` - Don't wait for user input (useful in scripts)

**Examples:**

```bash
blarg serve
blarg serve -p 8080
blarg serve -d ./build --no-tty
```

### `blarg gen`

Generate new pages or blog posts with frontmatter templates.

**Options:**

- `-b, --blog <title>` - Create a new blog post with the given title
- `-p, --page <path>` - Create a new page at the given path
- `-fm, --frontmatter <path>` - Add frontmatter to an existing file
- `-d, --dir <path>` - Site root directory (default: `./site`)

**Examples:**

```bash
# Create a blog post
blarg gen -b "My First Post"
# Creates: site/blog/YYYY-MM-DD-my-first-post.md

# Create a page
blarg gen -p about.md
# Creates: site/pages/about.md

# Create a nested page
blarg gen -p docs/getting-started.md
# Creates: site/pages/docs/getting-started.md

# Add frontmatter to existing file
blarg gen -fm content/old-post.md
```

## Site Configuration

### `blarg.json`

The `blarg.json` file in your site folder controls global site settings.

**Example:**

```json
{
  "siteTitle": "My Awesome Blog",
  "author": "Your Name",
  "navigation": [
    {
      "label": "Home",
      "href": "/"
    },
    {
      "label": "Blog",
      "href": "/latest.html"
    },
    {
      "label": "Tags",
      "href": "/tags.html"
    },
    {
      "label": "About",
      "href": "/about.html"
    }
  ],
  "dynamic": {
    "socialLinks": {
      "github": "https://github.com/yourusername"
    },
    "analytics": {
      "enabled": true,
      "trackingId": "UA-XXXXX-Y"
    }
  }
}
```

**Fields:**

- `siteTitle` - Your site's name (used in templates)
- `author` - Default author name
- `navigation` - Array of navigation links
    - `label` - Link text
    - `href` - Link URL
- `dynamic` - Free-form JSON object for custom template variables

**Accessing in Templates:**

```mustache
<title>{{config.siteTitle}}</title>
<p>By {{config.author}}</p>

{{#config.navigation}}
  <a href="{{href}}">{{label}}</a>
{{/config.navigation}}

{{#config.dynamic.socialLinks.github}}
  <a href="{{config.dynamic.socialLinks.github}}">GitHub</a>
{{/config.dynamic.socialLinks.github}}
```

## Frontmatter

Frontmatter is YAML metadata at the top of Markdown files, enclosed by `---`.

**Example:**

```markdown
---
title: My Blog Post
description: A brief description of the post
author: Jane Doe
published: 2025-01-15T10:00:00Z
lastUpdated: 2025-01-16T14:30:00Z
tags:
  - tutorial
  - markdown
  - web-development
---

# My Blog Post

Your content here...
```

**Fields:**

| Field         | Type          | Required | Description                          |
|---------------|---------------|----------|--------------------------------------|
| `title`       | String        | No       | Page or post title                   |
| `description` | String        | No       | Brief description/summary            |
| `author`      | String        | No       | Author name (overrides site default) |
| `published`   | ISO 8601 Date | No       | Publication date                     |
| `lastUpdated` | ISO 8601 Date | No       | Last modification date               |
| `tags`        | Array[String] | No       | List of tags for categorization      |

**Notes:**

- All fields are optional
- Dates must be in ISO 8601 format: `YYYY-MM-DDTHH:MM:SSZ`
- Tags are used to generate the tags page automatically
- Leave frontmatter empty with just `---` if no metadata needed

## Template System

Blarg uses Mustache for templating, with a hierarchical structure:

### Template Hierarchy

1. **Site Template** (`templates/site.mustache`)
    - Master layout wrapper
    - Contains `{{{content}}}` placeholder for page content

2. **Content Templates** (`templates/pages/*.mustache`)
    - Defines content structure
    - Wrapped by site template
    - Available templates:
        - `page.mustache` - Static pages
        - `blog.mustache` - Individual blog posts
        - `tags.mustache` - Tag listing page
        - `latest.mustache` - Recent posts page

3. **Partials** (`templates/partials/*.mustache`)
    - Reusable components
    - Inserted with `{{> partialName}}`
    - Default partials:
        - `header.mustache`
        - `nav.mustache`
        - `footer.mustache`

### Template Variables

**Global Context (available in all templates):**

```mustache
{{buildTime}}           <!-- Build timestamp (ISO 8601) -->
{{year}}                <!-- Current year (for copyright) -->

<!-- Configuration -->
{{config.siteTitle}}
{{config.author}}
{{#config.navigation}}
  {{label}}
  {{href}}
{{/config.navigation}}
{{config.dynamic.*}}    <!-- Any custom fields from blarg.json -->

<!-- Content (varies by page type) -->
{{{content}}}
```

**Page/Blog Content Context:**

```mustache
<!-- HTML content -->
{{{content.content}}}

<!-- Frontmatter -->
{{content.fm.title}}
{{content.fm.description}}
{{content.fm.author}}
{{content.fm.published}}      <!-- Formatted: "January 15, 2025" -->
{{content.fm.lastUpdated}}    <!-- Formatted: "January 16, 2025" -->
{{#content.fm.tags}}
  {{.}}                       <!-- Individual tag -->
{{/content.fm.tags}}

<!-- Metadata -->
{{content.href}}              <!-- Page URL -->
{{content.summary}}           <!-- First paragraph (for listings) -->
```

**Tags Page Context:**

```mustache
{{#content}}
  {{tag}}                     <!-- Tag name -->
  {{#articles}}
    {{fm.title}}
    {{href}}
    {{summary}}
  {{/articles}}
{{/content}}
```

**Latest Posts Page Context:**

```mustache
{{#content}}
  {{fm.title}}
  {{fm.published}}
  {{href}}
  {{summary}}
{{/content}}
```

### Example Site Template

```mustache
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>{{content.fm.title}} - {{config.siteTitle}}</title>
  <meta name="description" content="{{content.fm.description}}">
  <link rel="stylesheet" href="/css/style.css">
</head>
<body>
  {{> header}}
  {{> nav}}

  <main>
    {{{content}}}
  </main>

  {{> footer}}

  <script src="/js/main.js"></script>
</body>
</html>
```

### Example Blog Template

```mustache
<article>
  <header>
    <h1>{{content.fm.title}}</h1>
    <p class="meta">
      By {{content.fm.author}} on {{content.fm.published}}
      {{#content.fm.lastUpdated}}
        (Updated: {{content.fm.lastUpdated}})
      {{/content.fm.lastUpdated}}
    </p>
    {{#content.fm.tags}}
      <span class="tag">{{.}}</span>
    {{/content.fm.tags}}
  </header>

  <div class="content">
    {{{content.content}}}
  </div>
</article>
```

## Blog Posts

### File Naming

Blog posts must follow the date-based naming convention:

```
YYYY-MM-DD-slug.md
```

**Examples:**

- `2025-01-15-my-first-post.md`
- `2025-12-25-holiday-special.md`

### URL Structure

Blog posts are organized by date in the generated site:

```
site/blog/2025-01-15-my-first-post.md
  -> build/2025/01/15/my-first-post.html
```

### Nested Blog Posts

You can organize blog posts in subdirectories:

```
site/blog/tutorials/2025-01-15-getting-started.md
  -> build/tutorials/2025-01-15-getting-started.html
```

## Pages

Static pages in the `site/pages/` directory become root-level pages:

```
site/pages/about.md -> build/about.html
site/pages/index.md -> build/index.html
```

### Nested Pages

Create subdirectories for organized page structure:

```
site/pages/docs/guide.md -> build/docs/guide.html
site/pages/projects/web-app.md -> build/projects/web-app.html
```

## Static Files

Files in `site/static/` are copied to the build folder as-is:

```
site/static/css/style.css -> build/css/style.css
site/static/js/main.js -> build/js/main.js
site/static/img/logo.png -> build/img/logo.png
```

Reference static files in templates and Markdown using root-relative paths:

```html

<link rel="stylesheet" href="/css/style.css">
<img src="/img/logo.png" alt="Logo">
```

## Link Validation

Blarg automatically validates internal links during builds and reports broken links:

```
WARNING: Found 2 broken internal link(s):
  In /index.html:
    Link to '/missing-page' (tried: /missing-page.html) - page not found
  In /blog/2025/01/15/post.html:
    Link to '/about' (tried: /about.html) - page not found
```

**Link Resolution:**

- `/page` checks for `/page.html`, `/page/index.html`
- `/page.html` checks directly
- `/page/` checks for `/page/index.html`
- Static files are also validated (CSS, JS, images)
- External links (`http://`, `https://`, `mailto:`) are not validated
- Fragments (`#anchor`) and query strings (`?param=value`) are ignored
- Relative links (like `../other.html`) are not validated

## Development

Requires [Scala CLI](https://scala-cli.virtuslab.org/) and JVM 25+.

```sh
# Run locally from source
scala-cli run . -- build -d ./site

# Run tests
scala-cli test .
```

## Credits

Built with:

- [Scala 3](https://www.scala-lang.org/)
- [CommonMark Java](https://github.com/commonmark/commonmark-java) - Markdown parsing
- [Mustachio](https://github.com/alterationx10/mustachio) - Mustache templating
- [Ursula](https://github.com/alterationx10/ursula) - CLI framework
- [os-lib](https://github.com/com-lihaoyi/os-lib) - File system operations
- [Cask](https://github.com/com-lihaoyi/cask) - HTTP server
- [uPickle](https://github.com/com-lihaoyi/upickle) - JSON parsing

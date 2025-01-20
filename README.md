# Blarg!

A Markdown based static-site-generator.

## Rough layout of a website

- your-website/
    - site
        - pages
            - index.md
            - about.md
        - blog
            - yyyy-mm-dd-title.md
            - yyyy
                - mm
                    - dd
                        - title.md
        - static
            - css
            - js
            - img
        - templates
            - blog.mustache
            - latest.mustache
            - page.mustache
            - site.mustache
            - tags.mustache
        - blarg.json


## Blarg Commands

### new

Sets up a new templates site.

### gen

Generates new pages and blog posts. Also can add frontmatter to the beginning of an existing file.

### build

Builds the site. Can pass -w to watch for changes, and rebuild when they occur.

### serve

Serves the site from the build output directory.
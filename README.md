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

## Modes / Commands

- build
- dev
- gen
- serve
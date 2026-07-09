import MarkdownIt from 'markdown-it'

const markdown = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

const htmlPattern = /<\/?(p|div|section|article|h[1-6]|ul|ol|li|blockquote|pre|table|figure|img|span|strong|em)\b[\s\S]*>/i
const markdownPattern =
  /(^|\n)\s{0,3}(#{1,6}\s|[-*+]\s+|\d+\.\s+|>\s+|```)|\[[^\]]+\]\([^)]+\)|\*\*[^*]+\*\*/m

const normalizeMarkdown = (source = '') =>
  source
    .replace(/<!--\s*tabs:(start|end)\s*-->/gi, '')
    .replace(/<br\s*\/?>/gi, '\n')

export const renderMarkdown = (source = '') => markdown.render(normalizeMarkdown(source || ''))

export const renderContent = (source = '') => {
  if (!source) return ''
  if (markdownPattern.test(source)) {
    return renderMarkdown(source)
  }
  if (htmlPattern.test(source)) {
    return source
  }
  return renderMarkdown(source)
}

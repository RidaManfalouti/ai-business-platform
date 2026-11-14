import React from 'react';

interface MarkdownRendererProps {
  content: string;
}

export const MarkdownRenderer: React.FC<MarkdownRendererProps> = ({ content }) => {
  if (!content) return null;

  // Split lines into blocks
  const lines = content.split('\n');
  const blocks: React.ReactNode[] = [];

  let i = 0;
  let blockIndex = 0;

  while (i < lines.length) {
    const line = lines[i];

    // Code block ```
    if (line.trim().startsWith('```')) {
      const codeLines: string[] = [];
      i++;
      while (i < lines.length && !lines[i].trim().startsWith('```')) {
        codeLines.push(lines[i]);
        i++;
      }
      blocks.push(
        <pre
          key={`code-${blockIndex++}`}
          className="my-3 overflow-x-auto rounded-lg bg-slate-900 p-3.5 text-xs text-slate-100 font-mono border border-slate-700/60"
        >
          <code>{codeLines.join('\n')}</code>
        </pre>
      );
      i++;
      continue;
    }

    // Markdown Table
    if (line.trim().startsWith('|') && line.trim().endsWith('|')) {
      const tableLines: string[] = [];
      while (i < lines.length && lines[i].trim().startsWith('|') && lines[i].trim().endsWith('|')) {
        tableLines.push(lines[i]);
        i++;
      }

      if (tableLines.length >= 2) {
        const headerCells = tableLines[0]
          .split('|')
          .slice(1, -1)
          .map((c) => c.trim());

        const bodyLines = tableLines.slice(2); // Skip separator row

        blocks.push(
          <div key={`table-${blockIndex++}`} className="my-3 overflow-x-auto rounded-lg border border-slate-700">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-800 text-slate-200">
                <tr>
                  {headerCells.map((th, thIdx) => (
                    <th key={thIdx} className="px-3 py-2 font-semibold border-b border-slate-700">
                      {renderInline(th)}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-700/60 text-slate-300">
                {bodyLines.map((row, rIdx) => {
                  const cells = row
                    .split('|')
                    .slice(1, -1)
                    .map((c) => c.trim());
                  return (
                    <tr key={rIdx} className="hover:bg-slate-800/40">
                      {cells.map((cell, cIdx) => (
                        <td key={cIdx} className="px-3 py-2">
                          {renderInline(cell)}
                        </td>
                      ))}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        );
      }
      continue;
    }

    // Headers
    if (line.startsWith('### ')) {
      blocks.push(
        <h4 key={`h3-${blockIndex++}`} className="mt-4 mb-1.5 text-sm font-bold text-slate-100">
          {renderInline(line.substring(4))}
        </h4>
      );
      i++;
      continue;
    }
    if (line.startsWith('## ')) {
      blocks.push(
        <h3 key={`h2-${blockIndex++}`} className="mt-4 mb-2 text-base font-bold text-emerald-400">
          {renderInline(line.substring(3))}
        </h3>
      );
      i++;
      continue;
    }
    if (line.startsWith('# ')) {
      blocks.push(
        <h2 key={`h1-${blockIndex++}`} className="mt-4 mb-2 text-lg font-extrabold text-white">
          {renderInline(line.substring(2))}
        </h2>
      );
      i++;
      continue;
    }

    // Blockquote
    if (line.startsWith('> ')) {
      blocks.push(
        <blockquote
          key={`quote-${blockIndex++}`}
          className="my-2 border-l-4 border-emerald-500 pl-3 italic text-slate-300 text-xs"
        >
          {renderInline(line.substring(2))}
        </blockquote>
      );
      i++;
      continue;
    }

    // Unordered List (- or *)
    if (line.trim().startsWith('- ') || line.trim().startsWith('* ')) {
      const listItems: string[] = [];
      while (i < lines.length && (lines[i].trim().startsWith('- ') || lines[i].trim().startsWith('* '))) {
        listItems.push(lines[i].trim().substring(2));
        i++;
      }
      blocks.push(
        <ul key={`ul-${blockIndex++}`} className="my-2 list-disc list-inside space-y-1 text-xs text-slate-200">
          {listItems.map((item, idx) => (
            <li key={idx}>{renderInline(item)}</li>
          ))}
        </ul>
      );
      continue;
    }

    // Ordered List (1. 2. etc.)
    if (/^\d+\.\s/.test(line.trim())) {
      const listItems: string[] = [];
      while (i < lines.length && /^\d+\.\s/.test(lines[i].trim())) {
        listItems.push(lines[i].trim().replace(/^\d+\.\s/, ''));
        i++;
      }
      blocks.push(
        <ol key={`ol-${blockIndex++}`} className="my-2 list-decimal list-inside space-y-1 text-xs text-slate-200">
          {listItems.map((item, idx) => (
            <li key={idx}>{renderInline(item)}</li>
          ))}
        </ol>
      );
      continue;
    }

    // Empty lines
    if (!line.trim()) {
      i++;
      continue;
    }

    // Paragraph
    blocks.push(
      <p key={`p-${blockIndex++}`} className="my-1.5 text-xs text-slate-200 leading-relaxed">
        {renderInline(line)}
      </p>
    );
    i++;
  }

  return <div className="space-y-1">{blocks}</div>;
};

function renderInline(text: string): React.ReactNode {
  // Parse inline elements: **bold**, *italic*, `code`
  const parts: React.ReactNode[] = [];
  const regex = /(\*\*.*?\*\*|\*.*?\*|`.*?`)/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;

  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push(text.substring(lastIndex, match.index));
    }

    const token = match[0];
    if (token.startsWith('**') && token.endsWith('**')) {
      parts.push(
        <strong key={match.index} className="font-bold text-white">
          {token.substring(2, token.length - 2)}
        </strong>
      );
    } else if (token.startsWith('*') && token.endsWith('*')) {
      parts.push(
        <em key={match.index} className="italic text-slate-300">
          {token.substring(1, token.length - 1)}
        </em>
      );
    } else if (token.startsWith('`') && token.endsWith('`')) {
      parts.push(
        <code
          key={match.index}
          className="rounded bg-slate-800 px-1 py-0.5 text-[11px] font-mono text-emerald-300 border border-slate-700"
        >
          {token.substring(1, token.length - 1)}
        </code>
      );
    }
    lastIndex = regex.lastIndex;
  }

  if (lastIndex < text.length) {
    parts.push(text.substring(lastIndex));
  }

  return parts.length > 0 ? parts : text;
}

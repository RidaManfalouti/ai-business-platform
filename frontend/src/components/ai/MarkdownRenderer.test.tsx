import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import React from 'react';
import { MarkdownRenderer } from './MarkdownRenderer';

describe('MarkdownRenderer', () => {
  it('rend les titres h1, h2, h3 correctement', () => {
    const md = '# Titre 1\n## Titre 2\n### Titre 3';
    render(<MarkdownRenderer content={md} />);

    expect(screen.getByText('Titre 1')).toBeDefined();
    expect(screen.getByText('Titre 2')).toBeDefined();
    expect(screen.getByText('Titre 3')).toBeDefined();
  });

  it('rend le texte en gras et le code inline', () => {
    const md = 'Voici un texte avec **gras** et du `code_inline`.';
    render(<MarkdownRenderer content={md} />);

    expect(screen.getByText('gras')).toBeDefined();
    expect(screen.getByText('code_inline')).toBeDefined();
  });

  it('rend les listes à puces', () => {
    const md = '- Premier élément\n- Deuxième élément';
    render(<MarkdownRenderer content={md} />);

    expect(screen.getByText('Premier élément')).toBeDefined();
    expect(screen.getByText('Deuxième élément')).toBeDefined();
  });

  it('rend un tableau Markdown', () => {
    const md = `| Produit | Ventes |
| --- | --- |
| Laptop Pro | 42 |`;
    render(<MarkdownRenderer content={md} />);

    expect(screen.getByText('Produit')).toBeDefined();
    expect(screen.getByText('Ventes')).toBeDefined();
    expect(screen.getByText('Laptop Pro')).toBeDefined();
    expect(screen.getByText('42')).toBeDefined();
  });
});

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Input } from './Input';

describe('Input component', () => {
  it('renders input with label and placeholder', () => {
    render(<Input label="Nom du produit" placeholder="Saisir le nom" />);
    expect(screen.getByLabelText(/Nom du produit/i)).toBeInTheDocument();
    expect(screen.getByPlaceholderText('Saisir le nom')).toBeInTheDocument();
  });

  it('displays validation error message when error prop is provided', () => {
    render(<Input label="Email" error="Adresse email invalide" />);
    expect(screen.getByText('Adresse email invalide')).toBeInTheDocument();
  });

  it('handles user typing and onChange events', () => {
    const handleChange = vi.fn();
    render(<Input label="Recherche" onChange={handleChange} />);
    const input = screen.getByLabelText(/Recherche/i);
    fireEvent.change(input, { target: { value: 'MacBook' } });
    expect(handleChange).toHaveBeenCalled();
    expect((input as HTMLInputElement).value).toBe('MacBook');
  });
});

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Button } from './Button';

describe('Button component', () => {
  it('renders button with children', () => {
    render(<Button>Cliquez ici</Button>);
    expect(screen.getByRole('button', { name: 'Cliquez ici' })).toBeInTheDocument();
  });

  it('triggers onClick handler when clicked', () => {
    const handleClick = vi.fn();
    render(<Button onClick={handleClick}>Action</Button>);
    fireEvent.click(screen.getByRole('button', { name: 'Action' }));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('disables button and displays loading state when isLoading is true', () => {
    const handleClick = vi.fn();
    render(
      <Button isLoading onClick={handleClick}>
        Sauvegarder
      </Button>
    );

    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
    fireEvent.click(button);
    expect(handleClick).not.toHaveBeenCalled();
  });
});

import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ErrorMessage from '../../components/ErrorMessage';

describe('ErrorMessage', () => {
  it('should render the error message text', () => {
    render(<ErrorMessage message="Something went wrong" />);
    expect(screen.getByText('Something went wrong')).toBeInTheDocument();
  });

  it('should render with role="alert"', () => {
    render(<ErrorMessage message="Error occurred" />);
    expect(screen.getByRole('alert')).toBeInTheDocument();
  });

  it('should not render when message is empty', () => {
    const { container } = render(<ErrorMessage message="" />);
    expect(container.innerHTML).toBe('');
  });

  it('should render dismiss button when onDismiss is provided', () => {
    const onDismiss = vi.fn();
    render(<ErrorMessage message="Error" onDismiss={onDismiss} />);
    const button = screen.getByLabelText('关闭提示');
    expect(button).toBeInTheDocument();
  });

  it('should call onDismiss when dismiss button is clicked', async () => {
    const user = userEvent.setup();
    const onDismiss = vi.fn();
    render(<ErrorMessage message="Error" onDismiss={onDismiss} />);

    await user.click(screen.getByLabelText('关闭提示'));
    expect(onDismiss).toHaveBeenCalledTimes(1);
  });

  it('should not render dismiss button when onDismiss is not provided', () => {
    render(<ErrorMessage message="Error" />);
    expect(screen.queryByLabelText('关闭提示')).not.toBeInTheDocument();
  });

  it('should default to error type', () => {
    render(<ErrorMessage message="Default error" />);
    const alert = screen.getByRole('alert');
    // Error type has red-ish background
    expect(alert.style.backgroundColor).toBe('rgb(254, 242, 242)'); // #fef2f2
  });

  it('should render success type with appropriate styling', () => {
    render(<ErrorMessage type="success" message="Operation successful" />);
    const alert = screen.getByRole('alert');
    expect(alert.style.backgroundColor).toBe('rgb(240, 253, 244)'); // #f0fdf4
  });

  it('should render info type with appropriate styling', () => {
    render(<ErrorMessage type="info" message="Info message" />);
    const alert = screen.getByRole('alert');
    expect(alert.style.backgroundColor).toBe('rgb(239, 246, 255)'); // #eff6ff
  });

  it('should render warning type with appropriate styling', () => {
    render(<ErrorMessage type="warning" message="Warning message" />);
    const alert = screen.getByRole('alert');
    expect(alert.style.backgroundColor).toBe('rgb(255, 251, 235)'); // #fffbeb
  });
});

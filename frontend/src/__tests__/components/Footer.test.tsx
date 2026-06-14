import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import Footer from '../../components/Footer';

describe('Footer', () => {
  it('should display copyright information', () => {
    render(<Footer />);

    expect(screen.getByText(/CC91 论坛\. 版权所有\./)).toBeInTheDocument();
  });

  it('should display "Powered by" text', () => {
    render(<Footer />);

    expect(screen.getByText(/Powered by React \+ Spring Boot\./)).toBeInTheDocument();
  });

  it('should display footer links', () => {
    render(<Footer />);

    expect(screen.getByText('关于我们')).toBeInTheDocument();
    expect(screen.getByText('联系管理员')).toBeInTheDocument();
  });
});

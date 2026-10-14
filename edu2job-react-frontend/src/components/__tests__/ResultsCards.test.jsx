import '@testing-library/jest-dom';
import React from 'react';
import { render, screen } from '@testing-library/react';
import ResultsCards from '../ResultsCards';

describe('ResultsCards', () => {
  test('renders ranked roles with real scores as percentages', () => {
    render(<ResultsCards modelVersion="edu2job-v2" predictions={[
      { rank: 1, role: 'Data Analyst', score: 0.42 },
      { rank: 2, role: 'Software Engineer', score: 0.31 },
    ]} />);
    expect(screen.getByText('Data Analyst')).toBeInTheDocument();
    expect(screen.getByText(/42\.0%/)).toBeInTheDocument();
    expect(screen.getByText(/edu2job-v2/)).toBeInTheDocument();
  });

  test('never fabricates a score when the model provides none', () => {
    render(<ResultsCards predictions={[
      { rank: 1, role: 'Accountant', score: null },
    ]} />);
    expect(screen.getByText('Accountant')).toBeInTheDocument();
    expect(screen.getByText(/no confidence score provided/i)).toBeInTheDocument();
    expect(screen.queryByText(/%/)).not.toBeInTheDocument();
  });

  test('renders nothing for an empty list', () => {
    const { container } = render(<ResultsCards predictions={[]} />);
    expect(container).toBeEmptyDOMElement();
  });
});

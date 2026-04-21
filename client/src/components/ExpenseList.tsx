import { memo } from 'react';
import { formatMoney } from '../utils/money';
import { formatDate } from '../utils/date';
import type { Expense } from '../types/expense';

interface ExpenseListProps {
  expenses: Expense[];
  loading: boolean;
  error: string | null;
}

export function ExpenseList({ expenses, loading, error }: ExpenseListProps) {
  if (loading) {
    return (
      <div>
        {Array.from({ length: 5 }).map((_, i) => (
          <div key={i} className="skeleton skeleton-row" />
        ))}
      </div>
    );
  }

  if (error) {
    return (
      <div className="empty-state">
        <div className="empty-state-icon">⚠️</div>
        <div className="empty-state-title">Something went wrong</div>
        <div className="empty-state-text">{error}</div>
      </div>
    );
  }

  if (expenses.length === 0) {
    return (
      <div className="empty-state">
        <div className="empty-state-icon">📒</div>
        <div className="empty-state-title">No expenses yet</div>
        <div className="empty-state-text">Add your first expense to get started</div>
      </div>
    );
  }

  return (
    <table className="expense-table">
      <thead>
        <tr>
          <th>Date</th>
          <th>Category</th>
          <th>Description</th>
          <th style={{ textAlign: 'right' }}>Amount</th>
        </tr>
      </thead>
      <tbody>
        {expenses.map((expense) => (
          <ExpenseRow key={expense.id} expense={expense} />
        ))}
      </tbody>
    </table>
  );
}

/** Memoized row — only re-renders if the expense object changes */
const ExpenseRow = memo(function ExpenseRow({ expense }: { expense: Expense }) {
  return (
    <tr>
      <td data-label="Date" className="expense-date">
        {formatDate(expense.date)}
      </td>
      <td data-label="Category">
        <span className="category-badge">{expense.category}</span>
      </td>
      <td data-label="Description" className="expense-description">
        {expense.description || '—'}
      </td>
      <td data-label="Amount" className="expense-amount" style={{ textAlign: 'right' }}>
        {formatMoney(expense.amount)}
      </td>
    </tr>
  );
});

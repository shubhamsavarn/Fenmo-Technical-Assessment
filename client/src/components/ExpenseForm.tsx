import { useState } from 'react';
import { createExpense } from '../api/expenseApi';
import { getOrCreateIdempotencyKey, clearIdempotencyKey, refreshIdempotencyKey } from '../utils/idempotency';
import { todayISO } from '../utils/date';
import type { FormState } from '../types/expense';

const FORM_ID = 'create_expense';

interface ExpenseFormProps {
  onExpenseCreated: () => void;
  onToast: (message: string, type: 'success' | 'error') => void;
}

interface FormErrors {
  amount?: string;
  category?: string;
  date?: string;
}

export function ExpenseForm({ onExpenseCreated, onToast }: ExpenseFormProps) {
  const [amount, setAmount] = useState('');
  const [category, setCategory] = useState('');
  const [description, setDescription] = useState('');
  const [date, setDate] = useState(todayISO());
  const [formState, setFormState] = useState<FormState>('IDLE');
  const [errors, setErrors] = useState<FormErrors>({});

  // Client-side validation (defense layer 1 — server validates too)
  function validate(): FormErrors {
    const errs: FormErrors = {};

    if (!amount.trim()) {
      errs.amount = 'Amount is required';
    } else if (!/^\d+(\.\d{1,2})?$/.test(amount.trim())) {
      errs.amount = 'Enter a valid amount (e.g., 250.50)';
    } else if (parseFloat(amount) <= 0) {
      errs.amount = 'Amount must be positive';
    }

    if (!category.trim()) {
      errs.category = 'Category is required';
    }

    if (!date) {
      errs.date = 'Date is required';
    } else if (new Date(date) > new Date()) {
      errs.date = 'Date cannot be in the future';
    }

    return errs;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    // Client validation
    const validationErrors = validate();
    if (Object.keys(validationErrors).length > 0) {
      setErrors(validationErrors);
      return;
    }

    setErrors({});
    setFormState('SUBMITTING');

    const idempotencyKey = getOrCreateIdempotencyKey(FORM_ID);

    const result = await createExpense(
      {
        amount: amount.trim(),
        category: category.trim(),
        description: description.trim(),
        date,
      },
      idempotencyKey
    );

    if (result.ok) {
      // Success (201 Created) or idempotent replay (200 OK)
      clearIdempotencyKey(FORM_ID);
      setFormState('SUCCESS');
      onToast('Expense added successfully!', 'success');
      onExpenseCreated();

      // Reset form after brief visual feedback
      setTimeout(() => {
        setAmount('');
        setCategory('');
        setDescription('');
        setDate(todayISO());
        setFormState('IDLE');
        // Pre-generate next idempotency key
        refreshIdempotencyKey(FORM_ID);
      }, 1500);
    } else {
      const apiError = result.error;

      if (apiError.code === 'VALIDATION') {
        // Clear idempotency key — validation errors mean the key was "used" on a bad request
        clearIdempotencyKey(FORM_ID);
        refreshIdempotencyKey(FORM_ID);

        // Map server violations to form errors
        if (apiError.violations) {
          const serverErrors: FormErrors = {};
          apiError.violations.forEach((v) => {
            if (v.field === 'amount') serverErrors.amount = v.message;
            if (v.field === 'category') serverErrors.category = v.message;
            if (v.field === 'date') serverErrors.date = v.message;
          });
          setErrors(serverErrors);
        }
        onToast(apiError.message, 'error');
      } else if (apiError.code === 'NETWORK' || apiError.code === 'TIMEOUT' || apiError.code === 'SERVER') {
        // KEEP idempotency key — request may have been processed
        onToast(apiError.message, 'error');
      } else {
        clearIdempotencyKey(FORM_ID);
        refreshIdempotencyKey(FORM_ID);
        onToast(apiError.message, 'error');
      }

      setFormState('ERROR');
      setTimeout(() => setFormState('IDLE'), 100);
    }
  }

  const isSubmitting = formState === 'SUBMITTING';
  const isSuccess = formState === 'SUCCESS';

  return (
    <div className="card">
      <h2 className="card-title">Add Expense</h2>

      {isSuccess && (
        <div className="success-flash">✓ Expense added successfully</div>
      )}

      <form onSubmit={handleSubmit} noValidate>
        {/* Amount */}
        <div className="form-group">
          <label htmlFor="expense-amount" className="form-label">Amount</label>
          <div className="amount-prefix">
            <input
              id="expense-amount"
              type="text"
              inputMode="decimal"
              className={`form-input${errors.amount ? ' error' : ''}`}
              placeholder="0.00"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              disabled={isSubmitting}
              autoComplete="off"
            />
          </div>
          {errors.amount && <div className="form-error">⚠ {errors.amount}</div>}
        </div>

        {/* Category */}
        <div className="form-group">
          <label htmlFor="expense-category" className="form-label">Category</label>
          <input
            id="expense-category"
            type="text"
            className={`form-input${errors.category ? ' error' : ''}`}
            placeholder="e.g., food, transport, utilities"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            disabled={isSubmitting}
            autoComplete="off"
          />
          {errors.category && <div className="form-error">⚠ {errors.category}</div>}
        </div>

        {/* Description */}
        <div className="form-group">
          <label htmlFor="expense-description" className="form-label">
            Description <span style={{ color: 'var(--color-text-muted)' }}>(optional)</span>
          </label>
          <input
            id="expense-description"
            type="text"
            className="form-input"
            placeholder="What was this expense for?"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            disabled={isSubmitting}
            autoComplete="off"
          />
        </div>

        {/* Date */}
        <div className="form-group">
          <label htmlFor="expense-date" className="form-label">Date</label>
          <input
            id="expense-date"
            type="date"
            className={`form-input${errors.date ? ' error' : ''}`}
            value={date}
            max={todayISO()}
            onChange={(e) => setDate(e.target.value)}
            disabled={isSubmitting}
          />
          {errors.date && <div className="form-error">⚠ {errors.date}</div>}
        </div>

        {/* Submit */}
        <button
          type="submit"
          className="btn btn-primary"
          disabled={isSubmitting || isSuccess}
          id="submit-expense-btn"
        >
          {isSubmitting ? (
            <>
              <span className="spinner" /> Saving...
            </>
          ) : isSuccess ? (
            '✓ Saved'
          ) : (
            'Add Expense'
          )}
        </button>
      </form>
    </div>
  );
}

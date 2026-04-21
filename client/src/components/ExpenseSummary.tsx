import { formatMoney } from '../utils/money';
import type { SummaryMeta } from '../types/expense';

interface ExpenseSummaryProps {
  summary: SummaryMeta | null;
  loading: boolean;
}

export function ExpenseSummary({ summary, loading }: ExpenseSummaryProps) {
  if (loading || !summary) {
    return (
      <div className="summary-bar">
        <div className="skeleton" style={{ width: 180, height: 32 }} />
        <div className="skeleton" style={{ width: 100, height: 18 }} />
      </div>
    );
  }

  return (
    <div className="summary-bar">
      <div>
        <div className="summary-total">{formatMoney(summary.totalAmount)}</div>
        <div className="summary-count">
          Total across {summary.count} expense{summary.count !== 1 ? 's' : ''}
        </div>
      </div>
      <div style={{ fontSize: 'var(--font-size-xs)', color: 'var(--color-text-muted)' }}>
        {summary.currency}
      </div>
    </div>
  );
}

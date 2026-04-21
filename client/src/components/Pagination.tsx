import type { PageMeta } from '../types/expense';

interface PaginationProps {
  page: PageMeta | null;
  onPageChange: (page: number) => void;
}

export function Pagination({ page, onPageChange }: PaginationProps) {
  if (!page || page.totalPages <= 1) return null;

  const { number, totalPages, totalElements, size } = page;

  // Generate page numbers to display (max 5 visible)
  const pages: number[] = [];
  const start = Math.max(0, number - 2);
  const end = Math.min(totalPages - 1, start + 4);

  for (let i = start; i <= end; i++) {
    pages.push(i);
  }

  const from = number * size + 1;
  const to = Math.min((number + 1) * size, totalElements);

  return (
    <div className="pagination">
      <button
        className="pagination-btn"
        onClick={() => onPageChange(number - 1)}
        disabled={number === 0}
        aria-label="Previous page"
      >
        ‹
      </button>

      {pages.map((p) => (
        <button
          key={p}
          className={`pagination-btn${p === number ? ' active' : ''}`}
          onClick={() => onPageChange(p)}
          aria-label={`Page ${p + 1}`}
        >
          {p + 1}
        </button>
      ))}

      <button
        className="pagination-btn"
        onClick={() => onPageChange(number + 1)}
        disabled={number >= totalPages - 1}
        aria-label="Next page"
      >
        ›
      </button>

      <span className="pagination-info">
        {from}–{to} of {totalElements}
      </span>
    </div>
  );
}

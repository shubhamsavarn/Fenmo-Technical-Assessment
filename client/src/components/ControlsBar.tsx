import type { SortOption } from '../types/expense';

interface ControlsBarProps {
  categories: string[];
  selectedCategory: string;
  onCategoryChange: (cat: string) => void;
  sortOption: SortOption;
  onSortChange: (sort: SortOption) => void;
}

const SORT_LABELS: Record<SortOption, string> = {
  date_desc: '📅 Newest',
  date_asc: '📅 Oldest',
  amount_desc: '💰 Highest',
  amount_asc: '💰 Lowest',
};

export function ControlsBar({
  categories,
  selectedCategory,
  onCategoryChange,
  sortOption,
  onSortChange,
}: ControlsBarProps) {
  return (
    <div className="controls-bar">
      {/* Category Filter */}
      <select
        id="category-filter"
        className="form-select"
        value={selectedCategory}
        onChange={(e) => onCategoryChange(e.target.value)}
        aria-label="Filter by category"
      >
        <option value="">All Categories</option>
        {categories.map((cat) => (
          <option key={cat} value={cat}>
            {cat.charAt(0).toUpperCase() + cat.slice(1)}
          </option>
        ))}
      </select>

      {/* Sort Buttons */}
      <div className="sort-buttons">
        {(Object.entries(SORT_LABELS) as [SortOption, string][]).map(
          ([value, label]) => (
            <button
              key={value}
              className={`btn btn-sm btn-outline${sortOption === value ? ' active' : ''}`}
              onClick={() => onSortChange(value)}
              aria-label={`Sort by ${label}`}
            >
              {label}
            </button>
          )
        )}
      </div>
    </div>
  );
}

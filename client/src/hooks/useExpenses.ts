import { useState, useCallback, useEffect } from 'react';
import { getExpenses, getCategories } from '../api/expenseApi';
import type { Expense, SortOption, SummaryMeta, PageMeta } from '../types/expense';

interface UseExpensesReturn {
  expenses: Expense[];
  page: PageMeta | null;
  summary: SummaryMeta | null;
  categories: string[];
  loading: boolean;
  error: string | null;
  // Filters
  selectedCategory: string;
  setSelectedCategory: (cat: string) => void;
  sortOption: SortOption;
  setSortOption: (sort: SortOption) => void;
  currentPage: number;
  setCurrentPage: (page: number) => void;
  // Actions
  refetch: () => void;
}

export function useExpenses(): UseExpensesReturn {
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [page, setPage] = useState<PageMeta | null>(null);
  const [summary, setSummary] = useState<SummaryMeta | null>(null);
  const [categories, setCategories] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Filter state
  const [selectedCategory, setSelectedCategory] = useState('');
  const [sortOption, setSortOption] = useState<SortOption>('date_desc');
  const [currentPage, setCurrentPage] = useState(0);

  // Reset to page 0 when filters change
  const handleCategoryChange = useCallback((cat: string) => {
    setSelectedCategory(cat);
    setCurrentPage(0);
  }, []);

  const handleSortChange = useCallback((sort: SortOption) => {
    setSortOption(sort);
    setCurrentPage(0);
  }, []);

  const fetchExpenses = useCallback(async () => {
    setLoading(true);
    setError(null);

    const result = await getExpenses({
      category: selectedCategory || undefined,
      sort: sortOption,
      page: currentPage,
      size: 20,
    });

    if (result.ok) {
      setExpenses(result.data.content);
      setPage(result.data.page);
      setSummary(result.data.summary);
    } else {
      setError(result.error.message);
    }

    setLoading(false);
  }, [selectedCategory, sortOption, currentPage]);

  const fetchCategories = useCallback(async () => {
    const result = await getCategories();
    if (result.ok) {
      setCategories(result.data.categories);
    }
  }, []);

  // Fetch on mount and when filters change
  useEffect(() => {
    fetchExpenses();
  }, [fetchExpenses]);

  // Fetch categories on mount
  useEffect(() => {
    fetchCategories();
  }, [fetchCategories]);

  const refetch = useCallback(() => {
    fetchExpenses();
    fetchCategories();
  }, [fetchExpenses, fetchCategories]);

  return {
    expenses,
    page,
    summary,
    categories,
    loading,
    error,
    selectedCategory,
    setSelectedCategory: handleCategoryChange,
    sortOption,
    setSortOption: handleSortChange,
    currentPage,
    setCurrentPage,
    refetch,
  };
}

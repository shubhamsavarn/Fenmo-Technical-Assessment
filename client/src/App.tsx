import './index.css';
import './App.css';

import { ExpenseForm } from './components/ExpenseForm';
import { ExpenseList } from './components/ExpenseList';
import { ControlsBar } from './components/ControlsBar';
import { ExpenseSummary } from './components/ExpenseSummary';
import { Pagination } from './components/Pagination';
import { ToastContainer } from './components/ToastContainer';
import { useExpenses } from './hooks/useExpenses';
import { useToast } from './hooks/useToast';

function App() {
  const {
    expenses,
    page,
    summary,
    categories,
    loading,
    error,
    selectedCategory,
    setSelectedCategory,
    sortOption,
    setSortOption,
    setCurrentPage,
    refetch,
  } = useExpenses();

  const { toasts, addToast, dismissToast } = useToast();

  return (
    <>
      {/* Header */}
      <header className="app-header">
        <h1>Expense Tracker</h1>
        <span className="subtitle">Personal Finance</span>
      </header>

      {/* Main Content */}
      <main className="app-container">
        <div className="app-grid">
          {/* Left Column: Form */}
          <aside>
            <ExpenseForm
              onExpenseCreated={refetch}
              onToast={addToast}
            />
          </aside>

          {/* Right Column: List */}
          <section>
            {/* Summary */}
            <ExpenseSummary summary={summary} loading={loading} />

            {/* Filter & Sort Controls */}
            <ControlsBar
              categories={categories}
              selectedCategory={selectedCategory}
              onCategoryChange={setSelectedCategory}
              sortOption={sortOption}
              onSortChange={setSortOption}
            />

            {/* Expense Table */}
            <div className="card">
              <ExpenseList
                expenses={expenses}
                loading={loading}
                error={error}
              />
            </div>

            {/* Pagination */}
            <Pagination
              page={page}
              onPageChange={setCurrentPage}
            />
          </section>
        </div>
      </main>

      {/* Toast Notifications */}
      <ToastContainer toasts={toasts} onDismiss={dismissToast} />
    </>
  );
}

export default App;

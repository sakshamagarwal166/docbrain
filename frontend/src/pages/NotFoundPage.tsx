import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
      <div className="text-center">
        <h1 className="text-6xl font-bold text-primary-600 dark:text-primary-400">404</h1>
        <p className="mt-4 text-xl text-gray-600 dark:text-gray-300">Page not found</p>
        <p className="mt-2 text-gray-500 dark:text-gray-400">
          The page you're looking for doesn't exist.
        </p>
        <Link to="/documents" className="btn-primary inline-block mt-6">
          Go to Documents
        </Link>
      </div>
    </div>
  );
}

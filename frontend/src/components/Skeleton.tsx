export function SkeletonCard() {
  return (
    <div className="card p-4 animate-pulse">
      <div className="h-4 bg-gray-200 dark:bg-gray-700 rounded w-3/4 mb-3" />
      <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded w-1/2 mb-2" />
      <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded w-1/4" />
    </div>
  );
}

export function SkeletonList({ count = 3 }: { count?: number }) {
  return (
    <div className="space-y-3">
      {Array.from({ length: count }).map((_, i) => (
        <SkeletonCard key={i} />
      ))}
    </div>
  );
}

export function SkeletonChat() {
  return (
    <div className="space-y-4 p-4 animate-pulse">
      <div className="flex justify-end">
        <div className="h-10 bg-gray-200 dark:bg-gray-700 rounded-xl w-48" />
      </div>
      <div className="flex justify-start">
        <div className="h-20 bg-gray-200 dark:bg-gray-700 rounded-xl w-72" />
      </div>
      <div className="flex justify-end">
        <div className="h-10 bg-gray-200 dark:bg-gray-700 rounded-xl w-56" />
      </div>
    </div>
  );
}

import { useToast } from '../context/ToastContext';

const typeStyles = {
  success: 'bg-green-600 text-white',
  error: 'bg-red-600 text-white',
  info: 'bg-primary-600 text-white',
};

export default function ToastContainer() {
  const { toasts, removeToast } = useToast();

  if (toasts.length === 0) return null;

  return (
    <div className="fixed top-4 right-4 z-50 flex flex-col gap-2">
      {toasts.map((t) => (
        <div
          key={t.id}
          className={`${typeStyles[t.type]} px-4 py-3 rounded-lg shadow-lg max-w-sm
                      flex items-center justify-between gap-3 animate-slide-in`}
        >
          <span className="text-sm">{t.message}</span>
          <button
            onClick={() => removeToast(t.id)}
            className="text-white/80 hover:text-white text-lg leading-none"
          >
            &times;
          </button>
        </div>
      ))}
    </div>
  );
}

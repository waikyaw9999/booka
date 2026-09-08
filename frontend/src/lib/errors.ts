/** User-facing API / app errors. Technical detail stays in `cause` / console. */
export class AppError extends Error {
  readonly status?: number;
  readonly userMessage: string;

  constructor(userMessage: string, options?: { status?: number; cause?: unknown }) {
    super(userMessage);
    this.name = 'AppError';
    this.userMessage = userMessage;
    this.status = options?.status;
    if (options?.cause !== undefined) {
      (this as Error & { cause?: unknown }).cause = options.cause;
    }
  }
}

const GENERIC = 'Something went wrong. Please try again.';

const HTTP_REASON_PHRASES = new Set([
  'bad request',
  'unauthorized',
  'forbidden',
  'not found',
  'method not allowed',
  'conflict',
  'unprocessable entity',
  'too many requests',
  'internal server error',
  'bad gateway',
  'service unavailable',
  'gateway timeout',
]);

const TECHNICAL_RE =
  /Exception|SQLException|NullPointer|DataIntegrity|ConstraintViolation|stack\s*trace|\bat\s+[\w.$]+\(|\bcom\.[\w.]+|\borg\.(springframework|hibernate|postgresql)\b|\bjava\.[\w.]+|Internal Server Error|Request failed with status|Failed to fetch|NetworkError|ECONNREFUSED|ETIMEDOUT|SQL\s|syntax error|Jdbc|PSQLException|HttpMessageNotReadable|NestedServlet/i;

export function messageForStatus(status?: number): string {
  if (status == null) return GENERIC;
  if (status === 401) return 'Please sign in again.';
  if (status === 403) return "You don't have permission to do that.";
  if (status === 404) return "We couldn't find that.";
  if (status === 409) return 'That action conflicts with existing data.';
  if (status === 400 || status === 422) return 'Please check your input and try again.';
  if (status >= 500) return GENERIC;
  return GENERIC;
}

/** True when a server string is short and safe to show in the UI. */
export function isUserSafeMessage(message: unknown): message is string {
  if (typeof message !== 'string') return false;
  const trimmed = message.trim();
  if (!trimmed || trimmed.length > 220) return false;
  if (HTTP_REASON_PHRASES.has(trimmed.toLowerCase())) return false;
  if (TECHNICAL_RE.test(trimmed)) return false;
  if (/^\s*\{[\s\S]*\}\s*$/.test(trimmed)) return false;
  if (/https?:\/\/\S+/i.test(trimmed) && trimmed.length > 80) return false;
  return true;
}

function pickBodyMessage(body: unknown): string | undefined {
  if (!body || typeof body !== 'object') return undefined;
  const record = body as Record<string, unknown>;
  const candidates = [record.message, record.error, record.detail, record.title];
  for (const c of candidates) {
    if (typeof c === 'string' && c.trim()) return c.trim();
  }
  return undefined;
}

/**
 * Build a friendly AppError from an HTTP failure. Logs technical context to the console.
 */
export function normalizeHttpError(status: number, body: unknown, fallbackStatusText?: string): AppError {
  const raw = pickBodyMessage(body) ?? (fallbackStatusText?.trim() || undefined);
  // Prefer clean business/validation copy for 4xx; never surface raw 5xx exception text.
  const useServerMessage = status < 500 && isUserSafeMessage(raw);

  const userMessage = useServerMessage && raw ? raw : messageForStatus(status);

  console.error('[api]', { status, message: raw, body });
  return new AppError(userMessage, { status, cause: body ?? raw });
}

export function normalizeNetworkError(err: unknown): AppError {
  console.error('[api] network', err);
  return new AppError(GENERIC, { cause: err });
}

/** Safe string for InlineAlert / toasts / chat fallbacks. */
export function getErrorMessage(err: unknown, fallback = GENERIC): string {
  if (err instanceof AppError) return err.userMessage;
  if (err instanceof Error && isUserSafeMessage(err.message)) return err.message.trim();
  if (typeof err === 'string' && isUserSafeMessage(err)) return err.trim();
  if (err && typeof err === 'object' && 'userMessage' in err) {
    const um = (err as { userMessage: unknown }).userMessage;
    if (isUserSafeMessage(um)) return um.trim();
  }
  if (err && typeof err === 'object' && 'status' in err) {
    const status = Number((err as { status: unknown }).status);
    if (!Number.isNaN(status) && status > 0) return messageForStatus(status);
  }
  return fallback;
}

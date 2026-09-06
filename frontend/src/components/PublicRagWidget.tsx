import { useState } from 'react';
import { RagChat } from './RagChat';
import { api } from '../lib/api';
import { useRagStore } from '../stores';

interface PublicRagWidgetProps {
  hotelId: number | null;
}

export function PublicRagWidget({ hotelId }: PublicRagWidgetProps) {
  const { publicMessages, addPublicMessage } = useRagStore();
  const [loading, setLoading] = useState(false);
  const [ragAvailable, setRagAvailable] = useState<boolean | null>(null);

  async function handleSend(message: string) {
    addPublicMessage({ role: 'user', content: message });
    setLoading(true);
    try {
      const history = publicMessages.map((m) => ({ role: m.role, content: m.content }));
      const res = await api.publicRagChat(message, hotelId, history);
      setRagAvailable(res.ragAvailable);
      addPublicMessage({
        role: 'assistant',
        content: res.answer,
        citations: res.citations,
        availability: res.availability,
        hotelPath: res.hotelPath,
        blackouts: res.blackouts,
        propertySummary: res.propertySummary,
        bookingSummary: res.bookingSummary,
      });
    } catch (err) {
      addPublicMessage({
        role: 'assistant',
        content: err instanceof Error ? err.message : 'Assistant unavailable. Please try again.',
      });
    } finally {
      setLoading(false);
    }
  }

  return (
    <RagChat
      title="Concierge help"
      messages={publicMessages}
      onSend={handleSend}
      loading={loading}
      ragAvailable={ragAvailable}
      placeholder="Ask about amenities, rates, or availability…"
    />
  );
}

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { api, jwtStore } from '../../lib/api';
import { BookingFlow } from '../../components/BookingFlow';
import type { BookingResult, Doctor, Page, Slot } from '../../types';

// The whole network is mocked: BookingFlow issues every request via `api`.
vi.mock('../../lib/api', async () => {
  // Spread the real module (real ApiError class, real jwtStore) and replace
  // `api` with a controllable mock.
  const actual = await vi.importActual('../../lib/api');
  return {
    ...actual,
    api: vi.fn()
  };
});

const SLOTS: Slot[] = [
  {
    id: 's1',
    doctorId: 'd1',
    startAt: '2026-10-22T09:00:00Z',
    endAt: '2026-10-22T09:30:00Z',
    status: 'AVAILABLE'
  },
  {
    id: 's2',
    doctorId: 'd1',
    startAt: '2026-10-22T10:00:00Z',
    endAt: '2026-10-22T10:30:00Z',
    status: 'BOOKED'
  },
  {
    id: 's3',
    doctorId: 'd1',
    startAt: '2026-10-22T11:00:00Z',
    endAt: '2026-10-22T11:30:00Z',
    status: 'AVAILABLE'
  }
];

const DR: Doctor = {
  id: 'd1',
  userId: 'u2',
  name: 'Dr. Smith',
  specialization: 'Cardiology',
  consultationFee: 500,
  yearsExperience: 8,
  bio: 'Cardiologist',
  avatarUrl: null,
  ratings: 4.6,
  totalRatings: 120,
  slots: SLOTS
};

function slotPage(): Page<Slot> {
  return {
    content: SLOTS,
    totalElements: SLOTS.length,
    totalPages: 1,
    number: 0,
    size: 50,
    first: true,
    last: true
  };
}

function mockAvailability(): void {
  vi.mocked(api).mockImplementation((path: string) => {
    if (path.startsWith('/api/v1/doctors/d1/availability')) {
      return Promise.resolve(slotPage());
    }
    throw new Error('unexpected path ' + path);
  });
}

function mockBooking(replayed = false): void {
  const result: BookingResult = {
    consultationId: 'c1',
    slotId: 's1',
    paymentId: 'p1',
    status: 'PAID',
    replayed
  };
  vi.mocked(api).mockImplementation((path: string) => {
    if (path === '/api/v1/consultations/booking') {
      return Promise.resolve(result);
    }
    if (path.startsWith('/api/v1/doctors/d1/availability')) {
      return Promise.resolve(slotPage());
    }
    throw new Error('unexpected path ' + path);
  });
}

describe('BookingFlow', () => {
  beforeEach(() => {
    vi.mocked(api).mockReset();
    jwtStore.clear();
  });

  it('locks the confirm step until a doctor AND a slot are chosen', () => {
    mockAvailability();
    render(<BookingFlow doctors={[DR]} />);
    expect(screen.getByRole('button', { name: /confirm booking/i })).toBeDisabled();
  });

  it('offers only the AVAILABLE slots once a doctor is picked', async () => {
    mockAvailability();
    const user = userEvent.setup();
    render(<BookingFlow doctors={[DR]} />);

    await user.click(screen.getByLabelText('Dr. Smith'));

    // 1 doctor radio + 2 slot radios (s2 is BOOKED and filtered out).
    const radios = screen.getAllByRole('radio');
    expect(radios).toHaveLength(3);
  });

  it('submits a booking to /consultations/booking with the selected ids', async () => {
    mockBooking();
    const user = userEvent.setup();
    const onBooked = vi.fn();
    render(<BookingFlow doctors={[DR]} onBooked={onBooked} />);

    await user.click(screen.getByLabelText('Dr. Smith'));

    // s1 is the first available slot radio.
    const slotRadios = screen.getAllByRole('radio').slice(1);
    await user.click(slotRadios[0]);

    const confirm = screen.getByRole('button', { name: /confirm booking/i });
    expect(confirm).toBeEnabled();
    await user.click(confirm);

    const bookingCall = vi.mocked(api).mock.calls.find(
      ([path]) => path === '/api/v1/consultations/booking'
    );
    expect(bookingCall).toBeDefined();
    const options = bookingCall?.[1];
    expect(options?.method).toBe('POST');
    expect(options?.body).toMatchObject({
      doctorId: 'd1',
      slotId: 's1',
      paymentMethod: 'card'
    });

    expect(await screen.findByTestId('booking-success')).toBeInTheDocument();
    expect(onBooked).toHaveBeenCalledWith(
      expect.objectContaining({ consultationId: 'c1', paymentId: 'p1', status: 'PAID' })
    );
  });

  it('renders a replay message when the backend returns replayed=true', async () => {
    mockBooking(true);
    const user = userEvent.setup();
    render(<BookingFlow doctors={[DR]} />);

    await user.click(screen.getByLabelText('Dr. Smith'));
    const slotRadios = screen.getAllByRole('radio').slice(1);
    await user.click(slotRadios[0]);
    await user.click(screen.getByRole('button', { name: /confirm booking/i }));

    expect(await screen.findByTestId('booking-success')).toHaveTextContent(/replayed/i);
  });
});

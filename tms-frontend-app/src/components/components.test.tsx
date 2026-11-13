import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import {
  FilterChips,
  GlobalErrorBanner,
  PrimaryButton,
  RadioGroup,
  SliderToggle,
  StatusChip,
  TextInput,
} from '.';

describe('design system components', () => {
  it('Primary Button supports the disabled state', () => {
    render(<PrimaryButton disabled>Save</PrimaryButton>);
    expect(screen.getByRole('button', { name: 'Save' })).toBeDisabled();
  });

  it('Text Input exposes the error state accessibly', () => {
    render(<TextInput label="Email" error="Invalid email" />);
    const input = screen.getByLabelText('Email');
    expect(input).toHaveAttribute('aria-invalid', 'true');
    expect(input).toHaveAccessibleDescription('Invalid email');
  });

  it('Slider Toggle switches between Yes and No', async () => {
    function Harness() {
      const [checked, setChecked] = useState(false);
      return <SliderToggle label="Weekend availability" checked={checked} onChange={setChecked} />;
    }
    render(<Harness />);
    const toggle = screen.getByRole('switch', { name: 'Weekend availability' });
    expect(toggle).toHaveAttribute('aria-checked', 'false');
    await userEvent.click(toggle);
    expect(toggle).toHaveAttribute('aria-checked', 'true');
  });

  it('Radio Group selects a single option', async () => {
    function Harness() {
      const [value, setValue] = useState('internal');
      return (
        <RadioGroup
          label="Trainer type"
          value={value}
          onChange={setValue}
          options={[
            { value: 'internal', label: 'Internal' },
            { value: 'external', label: 'External' },
          ]}
        />
      );
    }
    render(<Harness />);
    await userEvent.click(screen.getByLabelText('External'));
    expect(screen.getByLabelText('External')).toBeChecked();
    expect(screen.getByLabelText('Internal')).not.toBeChecked();
  });

  it('Filter Chips reflect the active state', () => {
    render(
      <FilterChips
        label="Status"
        options={[
          { id: 'a', label: 'Active' },
          { id: 'b', label: 'Inactive' },
        ]}
        activeIds={['a']}
        onToggle={() => undefined}
      />,
    );
    expect(screen.getByRole('button', { name: 'Active' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Inactive' })).toHaveAttribute(
      'aria-pressed',
      'false',
    );
  });

  it('Status Chip and Global Error Banner render text labels and roles', () => {
    render(
      <>
        <StatusChip state="pending" label="Pending" />
        <GlobalErrorBanner variant="error" message="Load failed" />
      </>,
    );
    expect(screen.getByText('Pending')).toHaveClass('tms-chip--pending');
    expect(screen.getByRole('alert')).toHaveTextContent('Load failed');
  });
});

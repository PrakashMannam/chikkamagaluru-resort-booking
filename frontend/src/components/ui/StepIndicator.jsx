import React from 'react';
import './StepIndicator.css';

const DEFAULT_STEPS = [
  { number: 1, label: 'Guest Details' },
  { number: 2, label: 'Payment' },
  { number: 3, label: 'Confirmation' },
];

export function StepIndicator({ currentStep = 1, steps = DEFAULT_STEPS }) {
  return (
    <nav className="step-indicator" aria-label="Booking progress">
      <ol className="step-indicator__list">
        {steps.map((step) => {
          const isCompleted = step.number < currentStep;
          const isCurrent = step.number === currentStep;

          let statusClass = 'step-indicator__item--upcoming';
          if (isCompleted) statusClass = 'step-indicator__item--completed';
          if (isCurrent) statusClass = 'step-indicator__item--current';

          return (
            <li
              key={step.number}
              className={`step-indicator__item ${statusClass}`.trim()}
              aria-current={isCurrent ? 'step' : undefined}
            >
              <div className="step-indicator__circle" aria-hidden="true">
                {isCompleted ? '✓' : step.number}
              </div>
              <span className="step-indicator__label">{step.label}</span>
              {step.number < steps.length && (
                <div className="step-indicator__line" aria-hidden="true" />
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}

export default StepIndicator;

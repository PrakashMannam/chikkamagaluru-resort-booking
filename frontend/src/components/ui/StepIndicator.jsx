import React from 'react';
import './StepIndicator.css';

export function StepIndicator({ currentStep = 2 }) {
  const steps = [
    { number: 1, label: 'Select Sanctuary' },
    { number: 2, label: 'Guest Details' },
    { number: 3, label: 'Hold & Payment' },
    { number: 4, label: 'Confirmation' },
  ];

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

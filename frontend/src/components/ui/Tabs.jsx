import React, { useRef, useId, useState } from 'react';
import './Tabs.css';

export function Tabs({
  items = [],
  defaultTab,
  activeTab: controlledActiveTab,
  onChange,
  className = '',
}) {
  const baseId = useId();
  const [internalActiveTab, setInternalActiveTab] = useState(
    defaultTab || (items.length > 0 ? items[0].id : null)
  );

  const activeId = controlledActiveTab !== undefined ? controlledActiveTab : internalActiveTab;
  const tabListRef = useRef(null);

  function handleSelect(id) {
    if (controlledActiveTab === undefined) {
      setInternalActiveTab(id);
    }
    if (onChange) {
      onChange(id);
    }
  }

  function handleKeyDown(e, currentIndex) {
    const enabledItems = items.filter((item) => !item.disabled);
    const enabledCurrentIndex = enabledItems.findIndex((item) => item.id === items[currentIndex].id);

    let nextIndex = null;
    if (e.key === 'ArrowRight') {
      e.preventDefault();
      nextIndex = (enabledCurrentIndex + 1) % enabledItems.length;
    } else if (e.key === 'ArrowLeft') {
      e.preventDefault();
      nextIndex = (enabledCurrentIndex - 1 + enabledItems.length) % enabledItems.length;
    } else if (e.key === 'Home') {
      e.preventDefault();
      nextIndex = 0;
    } else if (e.key === 'End') {
      e.preventDefault();
      nextIndex = enabledItems.length - 1;
    }

    if (nextIndex !== null) {
      const nextId = enabledItems[nextIndex].id;
      handleSelect(nextId);
      const nextBtn = tabListRef.current?.querySelector(`[data-tab-id="${nextId}"]`);
      if (nextBtn) {
        nextBtn.focus();
      }
    }
  }

  const activeItem = items.find((item) => item.id === activeId) || items[0];

  return (
    <div className={`tabs ${className}`.trim()}>
      <div
        ref={tabListRef}
        role="tablist"
        aria-orientation="horizontal"
        className="tabs__list"
      >
        {items.map((item, index) => {
          const isSelected = item.id === activeId;
          const tabId = `${baseId}-tab-${item.id}`;
          const panelId = `${baseId}-panel-${item.id}`;

          return (
            <button
              key={item.id}
              id={tabId}
              type="button"
              role="tab"
              aria-selected={isSelected}
              aria-controls={panelId}
              tabIndex={isSelected ? 0 : -1}
              disabled={item.disabled}
              data-tab-id={item.id}
              onClick={() => handleSelect(item.id)}
              onKeyDown={(e) => handleKeyDown(e, index)}
              className={`tabs__trigger ${isSelected ? 'tabs__trigger--active' : ''}`.trim()}
            >
              {item.label}
            </button>
          );
        })}
      </div>
      {activeItem && (
        <div
          id={`${baseId}-panel-${activeItem.id}`}
          role="tabpanel"
          aria-labelledby={`${baseId}-tab-${activeItem.id}`}
          tabIndex={0}
          className="tabs__panel"
        >
          {activeItem.content}
        </div>
      )}
    </div>
  );
}

export default Tabs;

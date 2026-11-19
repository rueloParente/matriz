(() => {
  const root = document.documentElement;
  const system = window.matchMedia("(prefers-color-scheme: dark)");
  const storageKey = "jira-clone-theme";
  let preference;

  try {
    const stored = window.localStorage.getItem(storageKey);
    if (stored === "light" || stored === "dark") preference = stored;
  } catch {
    // Browser privacy settings may disable storage; the current page still works.
  }

  if (preference) root.dataset.theme = preference;

  document.addEventListener("DOMContentLoaded", () => {
    const button = document.getElementById("theme-toggle");
    if (!button) return;

    const isDark = () => preference ? preference === "dark" : system.matches;
    const updateButton = () => {
      const mode = isDark() ? "Light mode" : "Dark mode";
      button.textContent = mode;
      button.setAttribute("aria-label", "Switch to " + mode.toLowerCase());
    };

    updateButton();
    button.hidden = false;
    system.addEventListener("change", updateButton);
    button.addEventListener("click", () => {
      preference = isDark() ? "light" : "dark";
      root.dataset.theme = preference;
      updateButton();
      try {
        window.localStorage.setItem(storageKey, preference);
      } catch {
        // Keep the in-memory preference when persistence is unavailable.
      }
    });
  });
})();

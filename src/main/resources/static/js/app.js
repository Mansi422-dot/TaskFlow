document.addEventListener("DOMContentLoaded", () => {
  const modal = document.getElementById("taskModal");
  const openButton = document.querySelector("[data-open-modal]");
  const closeButtons = document.querySelectorAll("[data-close-modal]");

  function openModal() {
    if (!modal) return;
    modal.classList.add("open");
    modal.setAttribute("aria-hidden", "false");
    const title = modal.querySelector('input[name="title"]');
    if (title) title.focus();
  }
  function closeModal() {
    if (!modal) return;
    modal.classList.remove("open");
    modal.setAttribute("aria-hidden", "true");
  }

  if (openButton) openButton.addEventListener("click", openModal);
  closeButtons.forEach(button => button.addEventListener("click", closeModal));
  if (modal) {
    modal.addEventListener("click", event => {
      if (event.target === modal) closeModal();
    });
    document.addEventListener("keydown", event => {
      if (event.key === "Escape") closeModal();
    });
  }


  // Exact two-step checklist completion flow. Yes to the first prompt completes all checklist items.
  document.querySelectorAll("[data-confirm-complete]").forEach(form => {
    form.addEventListener("submit", event => {
      const card = form.closest(".task-card");
      const total = Number(card?.dataset.checklistTotal || 0);
      const completed = Number(card?.dataset.checklistCompleted || 0);
      if (total === 0 || completed >= total) return;
      event.preventDefault();

      const allItemsDone = window.confirm(`Are all the items in the checklist completed?\n\nSelect OK for Yes or Cancel for No.`);
      const allowField = form.querySelector('input[name="allowIncompleteChecklist"]');
      const markAllField = form.querySelector('input[name="markChecklistComplete"]');
      if (allItemsDone) {
        if (allowField) allowField.value = "false";
        if (markAllField) markAllField.value = "true";
        form.submit();
        return;
      }

      const completeAnyway = window.confirm(`Do you still want to mark this task as completed?\n\nSelect OK for Yes or Cancel for No.`);
      if (completeAnyway) {
        if (allowField) allowField.value = "true";
        if (markAllField) markAllField.value = "false";
        form.submit();
      }
    });
  });

  document.querySelectorAll("[data-confirm-delete]").forEach(form => {
    form.addEventListener("submit", event => {
      if (!window.confirm("Delete this task? This action cannot be undone.")) {
        event.preventDefault();
      }
    });
  });

  // Keep filter submission predictable: empty filters are omitted from the URL.
  document.querySelectorAll(".filters select").forEach(select => {
    select.addEventListener("change", () => {
      // Filters are submitted explicitly via the Filter button.
    });
  });
});


// Drag tasks between status columns and persist their status/order.
  let draggedCard = null;
  document.querySelectorAll('.task-card[draggable="true"]').forEach(card => {
    card.addEventListener('dragstart', event => { draggedCard = card; card.classList.add('dragging'); event.dataTransfer.effectAllowed = 'move'; event.dataTransfer.setData('text/plain', card.dataset.taskId); });
    card.addEventListener('dragend', () => { card.classList.remove('dragging'); draggedCard = null; document.querySelectorAll('.task-list').forEach(list => list.classList.remove('drop-target')); });
  });
  document.querySelectorAll('.task-list[data-status]').forEach(list => {
    list.addEventListener('dragover', event => { if (!draggedCard) return; event.preventDefault(); list.classList.add('drop-target'); const after = [...list.querySelectorAll('.task-card:not(.dragging)')].find(el => event.clientY < el.getBoundingClientRect().top + el.getBoundingClientRect().height / 2); if (after) list.insertBefore(draggedCard, after); else list.appendChild(draggedCard); });
    list.addEventListener('dragleave', event => { if (!list.contains(event.relatedTarget)) list.classList.remove('drop-target'); });
    list.addEventListener('drop', async event => { event.preventDefault(); list.classList.remove('drop-target'); if (!draggedCard) return; const status = list.dataset.status; let allowIncompleteChecklist = false; const total = Number(draggedCard.dataset.checklistTotal || 0); const completed = Number(draggedCard.dataset.checklistCompleted || 0); let markChecklistComplete = false; if (status === 'DONE' && total > 0 && completed < total) { const allDone = window.confirm('Are all the items in the checklist completed?\n\nSelect OK for Yes or Cancel for No.'); if (allDone) { markChecklistComplete = true; } else if (window.confirm('Do you still want to mark this task as completed?\n\nSelect OK for Yes or Cancel for No.')) { allowIncompleteChecklist = true; } else { window.location.reload(); return; } } const cards = [...list.querySelectorAll('.task-card')]; const position = cards.indexOf(draggedCard); try { const body = new URLSearchParams({status, position: String(position), allowIncompleteChecklist: String(allowIncompleteChecklist), markChecklistComplete: String(markChecklistComplete)}); const csrfToken = document.querySelector('meta[name="_csrf"]')?.content; const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content; const headers = {'Content-Type':'application/x-www-form-urlencoded'}; if (csrfToken && csrfHeader) headers[csrfHeader] = csrfToken; const response = await fetch(`/tasks/${draggedCard.dataset.taskId}/reorder`, {method:'POST', headers, body}); if (!response.ok) throw new Error('save failed'); draggedCard.dataset.taskStatus = status; window.location.reload(); } catch (e) { window.location.reload(); } });
  });

(() => {
    const LOCAL_TASKS_KEY = "todo.tasks.local";
    const body = document.body;
    const contextPath = sanitizeContextPath(body?.dataset.contextPath ?? "");
    let apiBase = "";
    let localTasks = readLocalTasks();

    const taskForm = document.getElementById("task-form");
    const taskInput = document.getElementById("task-input");
    const taskList = document.getElementById("task-list");

    if (!taskForm || !taskInput || !taskList) {
        return;
    }

    function sanitizeContextPath(value) {
        if (!value || value.includes("<%")) {
            return "";
        }
        return value.endsWith("/") ? value.slice(0, -1) : value;
    }

    function buildApiCandidates() {
        const candidates = [];
        const firstSegment = window.location.pathname.split("/").filter(Boolean)[0] || "";
        if (contextPath) {
            candidates.push(`${contextPath}/api/tasks`);
        }
        if (firstSegment) {
            candidates.push(`/${firstSegment}/api/tasks`);
        }
        candidates.push("/api/tasks");

        return Array.from(new Set(candidates));
    }

    async function detectApiBase() {
        const candidates = buildApiCandidates();
        for (const candidate of candidates) {
            try {
                const response = await fetch(`${candidate}/`, { method: "GET" });
                if (response.ok) {
                    apiBase = candidate;
                    return;
                }
            } catch (error) {
                // Try next candidate.
            }
        }

        apiBase = "";
    }

    function readLocalTasks() {
        try {
            const raw = localStorage.getItem(LOCAL_TASKS_KEY);
            if (!raw) {
                return [];
            }

            const parsed = JSON.parse(raw);
            return Array.isArray(parsed) ? parsed : [];
        } catch (error) {
            return [];
        }
    }

    function writeLocalTasks(tasks) {
        localTasks = tasks;
        localStorage.setItem(LOCAL_TASKS_KEY, JSON.stringify(tasks));
    }

    function nextLocalId() {
        return Date.now();
    }

    async function requestJson(path = "", options = {}) {
        const response = await fetch(`${apiBase}${path}`, {
            headers: { "Content-Type": "application/json" },
            ...options,
        });

        if (!response.ok) {
            const message = await readErrorMessage(response);
            throw new Error(message || `Request failed (${response.status})`);
        }

        const responseText = await response.text();
        return responseText ? JSON.parse(responseText) : null;
    }

    async function readErrorMessage(response) {
        try {
            const responseText = await response.text();
            if (!responseText) {
                return "";
            }
            const parsed = JSON.parse(responseText);
            return parsed.error || "";
        } catch (error) {
            return "";
        }
    }

    function renderTasks(tasks) {
        taskList.innerHTML = "";

        if (!Array.isArray(tasks) || tasks.length === 0) {
            const emptyItem = document.createElement("li");
            emptyItem.className = "empty-state";
            emptyItem.textContent = "No tasks yet. Add your first one.";
            taskList.appendChild(emptyItem);
            return;
        }

        const fragment = document.createDocumentFragment();

        tasks.forEach((task) => {
            const item = document.createElement("li");
            item.className = `task-item${task.completed ? " completed" : ""}`;

            const toggle = document.createElement("input");
            toggle.type = "checkbox";
            toggle.checked = Boolean(task.completed);
            toggle.setAttribute("aria-label", `Toggle task ${task.title}`);
            toggle.addEventListener("change", async () => {
                toggle.disabled = true;
                try {
                    if (apiBase) {
                        await requestJson(`/${task.id}`, {
                            method: "PUT",
                            body: JSON.stringify({ completed: toggle.checked }),
                        });
                        await loadTasks();
                    } else {
                        const updatedTasks = localTasks.map((entry) => {
                            if (entry.id !== task.id) {
                                return entry;
                            }

                            return { ...entry, completed: toggle.checked };
                        });
                        writeLocalTasks(updatedTasks);
                        renderTasks(updatedTasks);
                    }
                } catch (error) {
                    toggle.checked = !toggle.checked;
                    console.error(error);
                } finally {
                    toggle.disabled = false;
                }
            });

            const title = document.createElement("span");
            title.className = "task-title";
            title.textContent = task.title;

            const deleteButton = document.createElement("button");
            deleteButton.type = "button";
            deleteButton.className = "delete-btn";
            deleteButton.setAttribute("aria-label", `Delete task ${task.title}`);
            deleteButton.textContent = "Delete";
            deleteButton.addEventListener("click", async () => {
                deleteButton.disabled = true;
                try {
                    if (apiBase) {
                        await requestJson(`/${task.id}`, { method: "DELETE" });
                        await loadTasks();
                    } else {
                        writeLocalTasks(localTasks.filter((entry) => entry.id !== task.id));
                        renderTasks(localTasks);
                    }
                } catch (error) {
                    console.error(error);
                    deleteButton.disabled = false;
                }
            });

            item.append(toggle, title, deleteButton);
            fragment.appendChild(item);
        });

        taskList.appendChild(fragment);
    }

    async function loadTasks() {
        try {
            if (!apiBase) {
                renderTasks(localTasks);
                return;
            }

            const tasks = await requestJson("/");
            renderTasks(tasks || []);
        } catch (error) {
            console.error(error);
            renderTasks(localTasks);
        }
    }

    taskForm.addEventListener("submit", async (event) => {
        event.preventDefault();

        const title = taskInput.value.trim();
        if (!title) {
            taskInput.focus();
            return;
        }

        taskInput.disabled = true;

        try {
            if (apiBase) {
                await requestJson("/", {
                    method: "POST",
                    body: JSON.stringify({ title, completed: false }),
                });
            } else {
                const newTask = {
                    id: nextLocalId(),
                    title,
                    completed: false,
                };
                writeLocalTasks([...localTasks, newTask]);
            }

            taskForm.reset();
            await loadTasks();
            taskInput.focus();
        } catch (error) {
            console.error(error);
        } finally {
            taskInput.disabled = false;
        }
    });

    (async () => {
        await detectApiBase();
        await loadTasks();
    })();
})();

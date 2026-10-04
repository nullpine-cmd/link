"""
Вариант 15. Оценка погрешности вычисления функции

    z(x) = sh( sqrt(1 + x²) / (1 − x) ) / sin(x² + 0.4),    x = 0.20; 0.21; ...; 0.30

Функцию считаем «вручную» тремя приближёнными операциями:
    1) корень  t = sqrt(1 + x²)   — итерациями Герона;
    2) sh(t / (1 − x))            — отрезком ряда Тейлора;
    3) sin(x² + 0.4)              — отрезком ряда Тейлора.

Допустимую погрешность ε делим между этими операциями двумя способами
(принцип равных погрешностей и принцип равных влияний) и сравниваем
результат с «точным» значением из библиотеки math.

На графике по вертикали отложено −lg|f_T − f_пр| (примерно число верных
знаков после запятой). Точки выше пунктира −lg ε — точность достигнута.

Нужна библиотека matplotlib:  pip install matplotlib
"""

import math
import matplotlib.pyplot as plt


# ─────────────────────────── Исходные данные ───────────────────────────

X = [0.2 + 0.01 * i for i in range(11)]                # 0.20, 0.21, ..., 0.30
EPSILONS = [1e-6, 1e-7, 1e-8, 1e-9, 1e-10, 1e-11, 1e-12]

# Коэффициенты B_i из аналитического листка:
# во сколько раз ошибка каждой операции усиливается в итоговом ответе.
B_ROOT, B_SH, B_SIN = 8.2, 2.4, 12.5

A_MAX = 1.5    # наибольший аргумент sh на отрезке (при x = 0.3)
Y_MAX = 0.49   # наибольший аргумент sin на отрезке (при x = 0.3)


# ─────────────────────── Распределение погрешности ──────────────────────

def split_error(eps, principle):
    """Делит общую погрешность eps между тремя операциями.
    Возвращает три допустимые погрешности: для корня, для sh, для sin."""
    if principle == "влияний":
        # каждая операция даёт в ответ одинаковый вклад:  B_i · δ_i = eps / 3
        return eps / (3 * B_ROOT), eps / (3 * B_SH), eps / (3 * B_SIN)

    # все операции считаются с одной и той же δ:  (B_1 + B_2 + B_3) · δ = eps
    delta = eps / (B_ROOT + B_SH + B_SIN)
    return delta, delta, delta


def terms_needed(a, delta, divisor=1):
    """Наименьшее n, при котором оценка остатка ряда
           a^(2n+1) / (2n+1)! / divisor  ≤  delta.
    Для sh берём divisor = 3, для sin — divisor = 1 (см. аналитический листок)."""
    n = 0
    while a ** (2 * n + 1) / math.factorial(2 * n + 1) / divisor > delta:
        n += 1
    return n


# ─────────────────────── Приближённые вычисления ────────────────────────

def heron_sqrt(s, delta):
    """Корень из s по формуле Герона  w ← (w + s/w) / 2.
    Останавливаемся, когда два соседних приближения отличаются не больше чем на delta."""
    w = s
    while True:
        w_new = 0.5 * (w + s / w)
        if abs(w_new - w) <= delta:
            return w_new
        w = w_new


def odd_series(a, n, sign):
    """Сумма первых n + 1 членов ряда:
           sign = +1:   sh(a)  = a + a³/3! + a⁵/5! + ...
           sign = −1:   sin(a) = a − a³/3! + a⁵/5! − ...
    Следующий член получаем из предыдущего умножением на  sign · a² / (2k · (2k+1))."""
    term = total = a
    for k in range(1, n + 1):
        term *= sign * a * a / ((2 * k) * (2 * k + 1))
        total += term
    return total


def z_approx(x, delta_root, n_sh, n_sin):
    """Приближённое значение z(x): корень по Герону, sh и sin — рядами."""
    t = heron_sqrt(1 + x * x, delta_root)
    return odd_series(t / (1 - x), n_sh, +1) / odd_series(x * x + 0.4, n_sin, -1)


def z_exact(x):
    """«Точное» значение z(x) встроенными функциями."""
    return math.sinh(math.sqrt(1 + x * x) / (1 - x)) / math.sin(x * x + 0.4)


def minus_lg(error):
    """−lg|ошибки|. Если ошибка ровно 0 (совпали все знаки), логарифма нет:
    возвращаем nan, и matplotlib просто не рисует эту точку."""
    return -math.log10(error) if error > 0 else math.nan


# ──────────────────────────────── Расчёт ────────────────────────────────

PRINCIPLES = ["погрешностей", "влияний"]
z_true = [z_exact(x) for x in X]       # точные значения не зависят от ε — считаем один раз

accuracy = {}                          # (ε, принцип) → список −lg|f_T − f_пр| по всем x

print("    ε     принцип равных   N_sh  N_sin")
for eps in EPSILONS:
    for principle in PRINCIPLES:
        # число членов рядов зависит только от ε и принципа, а не от x
        d_root, d_sh, d_sin = split_error(eps, principle)
        n_sh = terms_needed(A_MAX, d_sh, divisor=3)
        n_sin = terms_needed(Y_MAX, d_sin)
        print(f"  {eps:.0e}  {principle:<14} {n_sh:5} {n_sin:5}")

        accuracy[eps, principle] = [
            minus_lg(abs(zt - z_approx(x, d_root, n_sh, n_sin)))
            for x, zt in zip(X, z_true)
        ]


# ──────────────────────────────── График ────────────────────────────────

# Спокойное оформление: светлая сетка, серые подписи, без верхней и правой рамки.
plt.rcParams.update({
    "axes.spines.top": False,
    "axes.spines.right": False,
    "axes.edgecolor": "#c3c2b7",
    "axes.grid": True,
    "grid.color": "#e1e0d9",
    "grid.linewidth": 0.8,
    "xtick.major.size": 0,
    "ytick.major.size": 0,
    "xtick.labelcolor": "#52514e",
    "ytick.labelcolor": "#52514e",
    "axes.titlesize": 11,
})

# Оранжевую линию рисуем первой и толще, чтобы при совпадении была видна и синяя поверх.
STYLE = {
    "погрешностей": dict(color="#eb6834", lw=3, marker="s", markersize=6),
    "влияний":      dict(color="#2a78d6", lw=1.5, marker="o", markersize=4),
}

# 7 значений ε → сетка 2 × 4; восьмая клетка уходит под легенду.
fig, axes = plt.subplots(2, 4, figsize=(12, 6), sharex=True, sharey=True,
                         layout="constrained")
fig.get_layout_engine().set(wspace=0.06, hspace=0.08)   # воздух между панелями

for ax, eps in zip(axes.flat, EPSILONS):
    for principle in PRINCIPLES:
        ax.plot(X, accuracy[eps, principle], label=f"принцип равных {principle}", **STYLE[principle])
    ax.axhline(-math.log10(eps), color="#898781", lw=1, ls="--", label="требуемая точность −lg ε")
    ax.set_title(f"ε = $10^{{{round(math.log10(eps))}}}$")

axes[0, 0].set_xticks([0.20, 0.25, 0.30])      # ось x у всех панелей общая
axes[0, 3].tick_params(labelbottom=True)       # под этой панелью легенда, вернём подписи x

legend_cell = axes[1, 3]
legend_cell.axis("off")
legend_cell.legend(*axes[0, 0].get_legend_handles_labels(),
                   loc="center", frameon=False)

fig.supxlabel("x")
fig.supylabel(r"$-\lg\,|f_T - f_{пр}|$")
fig.savefig("variant15.png", dpi=150, bbox_inches="tight")
plt.show()

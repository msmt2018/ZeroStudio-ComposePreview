# classes6.dex

.class public final Landroid/zero/studio/compose/preview/resources/databinding/TwoTextInputLayoutDropdownBinding;
.super Ljava/lang/Object;
.source "TwoTextInputLayoutDropdownBinding.java"

# interfaces
.implements Landroidx/viewbinding/ViewBinding;


# instance fields
.field private final rootView:Landroidx/coordinatorlayout/widget/CoordinatorLayout;

.field public final textInputLayout:Lcom/google/android/material/textfield/TextInputLayout;

.field public final textInputLayoutSecond:Lcom/google/android/material/textfield/TextInputLayout;


# direct methods
.method private constructor <init>(Landroidx/coordinatorlayout/widget/CoordinatorLayout;Lcom/google/android/material/textfield/TextInputLayout;Lcom/google/android/material/textfield/TextInputLayout;)V
    .registers 4
    .param p1, "rootView"  # Landroidx/coordinatorlayout/widget/CoordinatorLayout;
    .param p2, "textInputLayout"  # Lcom/google/android/material/textfield/TextInputLayout;
    .param p3, "textInputLayoutSecond"  # Lcom/google/android/material/textfield/TextInputLayout;

    .line 29
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    .line 30
    iput-object p1, p0, Landroid/zero/studio/compose/preview/resources/databinding/TwoTextInputLayoutDropdownBinding;->rootView:Landroidx/coordinatorlayout/widget/CoordinatorLayout;

    .line 31
    iput-object p2, p0, Landroid/zero/studio/compose/preview/resources/databinding/TwoTextInputLayoutDropdownBinding;->textInputLayout:Lcom/google/android/material/textfield/TextInputLayout;

    .line 32
    iput-object p3, p0, Landroid/zero/studio/compose/preview/resources/databinding/TwoTextInputLayoutDropdownBinding;->textInputLayoutSecond:Lcom/google/android/material/textfield/TextInputLayout;

    .line 33
    return-void
.end method

.method public static bind(Landroid/view/View;)Landroid/zero/studio/compose/preview/resources/databinding/TwoTextInputLayoutDropdownBinding;
    .registers 6
    .param p0, "rootView"  # Landroid/view/View;

    .line 62
    sget v0, Landroid/zero/studio/compose/preview/resources/R$id;->text_input_layout:I

    .line 63
    .local v0, "id":I
    invoke-static {p0, v0}, Landroidx/viewbinding/ViewBindings;->findChildViewById(Landroid/view/View;I)Landroid/view/View;

    move-result-object v1

    check-cast v1, Lcom/google/android/material/textfield/TextInputLayout;

    .line 64
    .local v1, "textInputLayout":Lcom/google/android/material/textfield/TextInputLayout;
    if-eqz v1, :cond_1e

    .line 68
    sget v0, Landroid/zero/studio/compose/preview/resources/R$id;->text_input_layout_second:I

    .line 69
    invoke-static {p0, v0}, Landroidx/viewbinding/ViewBindings;->findChildViewById(Landroid/view/View;I)Landroid/view/View;

    move-result-object v2

    check-cast v2, Lcom/google/android/material/textfield/TextInputLayout;

    .line 70
    .local v2, "textInputLayoutSecond":Lcom/google/android/material/textfield/TextInputLayout;
    if-eqz v2, :cond_1d

    .line 74
    new-instance v3, Landroid/zero/studio/compose/preview/resources/databinding/TwoTextInputLayoutDropdownBinding;

    move-object v4, p0

    check-cast v4, Landroidx/coordinatorlayout/widget/CoordinatorLayout;

    invoke-direct {v3, v4, v1, v2}, Landroid/zero/studio/compose/preview/resources/databinding/TwoTextInputLayoutDropdownBinding;-><init>(Landroidx/coordinatorlayout/widget/CoordinatorLayout;Lcom/google/android/material/textfield/TextInputLayout;Lcom/google/android/material/textfield/TextInputLayout;)V

    return-object v3

    .line 71
    :cond_1d
    goto :goto_1f

    .line 65
    .end local v2  # "textInputLayoutSecond":Lcom/google/android/material/textfield/TextInputLayout;
    :cond_1e
    nop

    .line 77
    .end local v1  # "textInputLayout":Lcom/google/android/material/textfield/TextInputLayout;
    :goto_1f
    invoke-virtual {p0}, Landroid/view/View;->getResources()Landroid/content/res/Resources;

    move-result-object v1

    invoke-virtual {v1, v0}, Landroid/content/res/Resources;->getResourceName(I)Ljava/lang/String;

    move-result-object v1

    .line 78
    .local v1, "missingId":Ljava/lang/String;
    new-instance v2, Ljava/lang/NullPointerException;

    const-string v3, "Missing required view with ID: "

    invoke-virtual {v3, v1}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;

    move-result-object v3

    invoke-direct {v2, v3}, Ljava/lang/NullPointerException;-><init>(Ljava/lang/String;)V

    throw v2
.end method

.method public static inflate(Landroid/view/LayoutInflater;)Landroid/zero/studio/compose/preview/resources/databinding/TwoTextInputLayoutDropdownBinding;
    .registers 3
    .param p0, "inflater"  # Landroid/view/LayoutInflater;

    .line 43
    const/4 v0, 0x0

    const/4 v1, 0x0

    invoke-static {p0, v0, v1}, Landroid/zero/studio/compose/preview/resources/databinding/TwoTextInputLayoutDropdownBinding;->inflate(Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Z)Landroid/zero/studio/compose/preview/resources/databinding/TwoTextInputLayoutDropdownBinding;

    move-result-object v0

    return-object v0
.end method

.method public static inflate(Landroid/view/LayoutInflater;Landroid/view/ViewGroup;Z)Landroid/zero/studio/compose/preview/resources/databinding/TwoTextInputLayoutDropdownBinding;
    .registers 5
    .param p0, "inflater"  # Landroid/view/LayoutInflater;
    .param p1, "parent"  # Landroid/view/ViewGroup;
    .param p2, "attachToParent"  # Z

    .line 49
    sget v0, Landroid/zero/studio/compose/preview/resources/R$layout;->two_text_input_layout_dropdown:I

    const/4 v1, 0x0

    invoke-virtual {p0, v0, p1, v1}, Landroid/view/LayoutInflater;->inflate(ILandroid/view/ViewGroup;Z)Landroid/view/View;

    move-result-object v0

    .line 50
    .local v0, "root":Landroid/view/View;
    if-eqz p2, :cond_c

    .line 51
    invoke-virtual {p1, v0}, Landroid/view/ViewGroup;->addView(Landroid/view/View;)V

    .line 53
    :cond_c
    invoke-static {v0}, Landroid/zero/studio/compose/preview/resources/databinding/TwoTextInputLayoutDropdownBinding;->bind(Landroid/view/View;)Landroid/zero/studio/compose/preview/resources/databinding/TwoTextInputLayoutDropdownBinding;

    move-result-object v1

    return-object v1
.end method


# virtual methods
.method public bridge synthetic getRoot()Landroid/view/View;
    .registers 2

    .line 18
    invoke-virtual {p0}, Landroid/zero/studio/compose/preview/resources/databinding/TwoTextInputLayoutDropdownBinding;->getRoot()Landroidx/coordinatorlayout/widget/CoordinatorLayout;

    move-result-object v0

    return-object v0
.end method

.method public getRoot()Landroidx/coordinatorlayout/widget/CoordinatorLayout;
    .registers 2

    .line 38
    iget-object v0, p0, Landroid/zero/studio/compose/preview/resources/databinding/TwoTextInputLayoutDropdownBinding;->rootView:Landroidx/coordinatorlayout/widget/CoordinatorLayout;

    return-object v0
.end method
